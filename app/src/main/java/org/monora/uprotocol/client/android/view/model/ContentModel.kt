package org.monora.uprotocol.client.android.view.model

import com.genonbeta.android.framework.util.actionperformer.SelectionModel
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend

interface ContentModel : SelectionModel {
    fun canCopy(): Boolean

    fun canMove(): Boolean

    fun canShare(): Boolean

    fun canRemove(): Boolean

    fun canRename(): Boolean

    fun copy(operationBackend: OperationBackend, destination: Destination): Boolean

    fun dateCreated(): Long

    fun dateModified(): Long

    fun filter(charSequence: CharSequence): Boolean

    fun length(): Long

    fun move(operationBackend: OperationBackend, destination: Destination): Boolean

    fun name(): String

    fun remove(operationBackend: OperationBackend): Boolean

    fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean
}