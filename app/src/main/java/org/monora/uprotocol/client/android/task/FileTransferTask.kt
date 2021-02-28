/*
 * Copyright (C) 2020 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.monora.uprotocol.client.android.task

import android.content.Context
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.model.Identifier.Companion.from
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.model.Identity.Companion.withANDs
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.monora.uprotocol.client.android.task.transfer.MainTransferOperation
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.Transfers
import java.io.IOException

class FileTransferTask(
    private val bridge: CommunicationBridge,
    val transfer: Transfer,
    val client: Client,
    val type: TransferItem.Type,
) : AttachableAsyncTask<AttachedTaskListener>() {
    // TODO: 2/25/21 Generate via dependency injection
    var operation: MainTransferOperation = MainTransferOperation(this)

    @Throws(TaskStoppedException::class)
    override fun onRun() {
        if (TransferItem.Type.Outgoing == type) {
            Transfers.receive(bridge, operation, transfer.id)
        } else if (TransferItem.Type.Incoming == type) {
            Transfers.send(bridge, operation, transfer.id)
        }
    }

    override fun onPublishStatus() {
        super.onPublishStatus()
        if (interrupted() || finished) {
            if (interrupted()) {
                ongoingContent = context.getString(R.string.text_cancellingTransfer)
            }
            return
        }
        val bytesTransferred = operation.bytesTotal + operation.bytesOngoing
        val text = StringBuilder()
        progress.progress?.total = 100

        // FIXME: 2/25/21 Show the eta
        /*
        if (bytesTransferred > 0 && index.bytesPending() > 0) {
            progress.progress?.progress = (100 * (bytesTransferred.toDouble() / index.bytesPending())).toInt()
        }
        if (lastMovedBytes > 0 && bytesTransferred > 0) {
            val change = bytesTransferred - lastMovedBytes
            text.append(Files.formatLength(change, false))
            if (index.bytesPending() > 0 && change > 0) {
                val timeNeeded: Long = (index.bytesPending() - bytesTransferred) / change
                text.append(" (")
                text.append(
                    context.getString(
                        R.string.text_remainingTime,
                        TimeUtils.getDuration(timeNeeded, false)
                    )
                )
                text.append(")")
            }
        }
        lastMovedBytes = bytesTransferred*/
        operation.ongoing?.also {
            if (text.isNotEmpty()) text.append(" ").append(context.getString(R.string.mode_middleDot)).append(" ")
            text.append(it.itemName)
            /*try {
                val flag = TransferItem.Flag.IN_PROGRESS
                flag.bytesValue = currentBytes

                if (TransferItem.Type.INCOMING == type) {
                    it.flag = flag
                } else if (TransferItem.Type.OUTGOING == type) {
                    it.putFlag(device.uid, flag)
                }

                kuick.update(database, it, transfer, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }*/
        }
        ongoingContent = text.toString()
    }

    override fun forceQuit() {
        super.forceQuit()
        try {
            bridge.activeConnection.socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override val identity: Identity
        get() = identityOf(this)

    override fun getName(context: Context): String {
        return context.getString(R.string.text_transfer)
    }

    override fun interrupt(userAction: Boolean): Boolean {
        try {
            bridge.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return super.interrupt(userAction)
    }

    enum class Id {
        TransferId, ClientUid, Type
    }

    companion object {
        val TAG = FileTransferTask::class.java.simpleName

        fun identityOf(task: FileTransferTask): Identity {
            return identifyWith(task.transfer.id, task.client.clientUid, task.type)
        }

        fun identifyWith(transferId: Long): Identity {
            return withANDs(from(Id.TransferId, transferId))
        }

        fun identifyWith(transferId: Long, type: TransferItem.Type?): Identity {
            return withANDs(from(Id.TransferId, transferId), from(Id.Type, type))
        }

        fun identifyWith(transferId: Long, deviceId: String?): Identity {
            return withANDs(from(Id.TransferId, transferId), from(Id.ClientUid, deviceId))
        }

        fun identifyWith(transferId: Long, clientUid: String?, type: TransferItem.Type?): Identity {
            return withANDs(
                from(Id.TransferId, transferId),
                from(Id.ClientUid, clientUid),
                from(Id.Type, type)
            )
        }
    }
}