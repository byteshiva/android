/*
 * Copyright (C) 2021 Veli Tasalı
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

package org.monora.uprotocol.client.android.fragment.pickclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.databinding.LayoutClientDetailPickBinding
import org.monora.uprotocol.client.android.databinding.LayoutManualConnectionBinding
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.protocol.communication.ProtocolException
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class ManualConnectionFragment : Fragment(R.layout.layout_manual_connection) {
    private val viewModel: ManualConnectionViewModel by viewModels()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutManualConnectionBinding.bind(view)

        binding.confirmButton.setOnClickListener {
            val address = binding.editText.text?.trim()?.toString()

            if (address.isNullOrEmpty()) {
                binding.editText.error = getString(R.string.mesg_enterValidHostAddress)
            } else {
                viewModel.connect(address)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is ManualConnectionState.Loading -> {

                }
                is ManualConnectionState.Error -> when (it.exception) {
                    is UnknownHostException -> binding.editText.error = getString(R.string.mesg_unknownHostError)
                    is UnauthorizedClientException -> binding.editText.error = getString(R.string.mesg_notAllowed)
                    else -> binding.editText.error = it.exception.message
                }
                is ManualConnectionState.Loaded -> {
                    val pickBinding = LayoutClientDetailPickBinding.inflate(
                        LayoutInflater.from(context), null, false
                    )
                    val bottomSheetDialog = BottomSheetDialog(requireActivity())

                    pickBinding.viewModel = ClientContentViewModel(it.clientRoute.client)
                    pickBinding.acceptButton.setOnClickListener { _ ->
                        pickBinding.acceptButton.isEnabled = false
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                clientPickerViewModel.bridge.postValue(viewModel.reconnect(it.clientRoute))
                            } catch (e: Exception) {

                            } finally {
                                lifecycleScope.launchWhenResumed {
                                    pickBinding.acceptButton.isEnabled = true
                                }
                            }
                        }
                    }
                    pickBinding.rejectButton.setOnClickListener {
                        bottomSheetDialog.dismiss()
                    }

                    pickBinding.executePendingBindings()

                    bottomSheetDialog.setContentView(pickBinding.root)
                    bottomSheetDialog.show()
                }
            }

            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
            binding.progressBar.visibility = if (it.loading) View.VISIBLE else View.GONE
            binding.confirmButton.isEnabled = !it.loading
        }
    }
}

@HiltViewModel
class ManualConnectionViewModel @Inject internal constructor(
    private var connectionFactory: ConnectionFactory,
    private var persistenceProvider: PersistenceProvider,
) : ViewModel() {
    private val _state = MutableLiveData<ManualConnectionState>()

    private var _job: Job? = null

    val state = liveData {
        emitSource(_state)
    }

    fun connect(address: String) = _job ?: viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(ManualConnectionState.Loading())

        try {
            val inetAddress = InetAddress.getByName(address)
            val bridge = CommunicationBridge.connect(
                connectionFactory, persistenceProvider, inetAddress
            )

            if (!bridge.requestAcquaintance()) {
                throw ProtocolException()
            }

            val client = bridge.remoteClient
            val clientAddress = bridge.remoteClientAddress

            if (client !is UClient || clientAddress !is UClientAddress) {
                throw UnsupportedOperationException("Unknown parameters")
            }

            _state.postValue(ManualConnectionState.Loaded(ClientRoute(client, clientAddress)))
        } catch (e: Exception) {
            e.printStackTrace()
            _state.postValue(ManualConnectionState.Error(e))
        } finally {
            _job = null
        }
    }.also { _job = it }

    fun reconnect(clientRoute: ClientRoute): CommunicationBridge = CommunicationBridge.Builder(
        connectionFactory, persistenceProvider, clientRoute.address.inetAddress
    ).also {
        it.setClientUid(clientRoute.client.clientUid)
    }.connect()
}

sealed class ManualConnectionState(val loading: Boolean) {
    class Loading : ManualConnectionState(true)

    class Error(val exception: Exception) : ManualConnectionState(false)

    class Loaded(val clientRoute: ClientRoute) : ManualConnectionState(false)
}