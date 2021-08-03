/*
 * Copyright (C) 2019 Veli Tasalı
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
package org.monora.uprotocol.client.android.activity

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToDrawable
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel

/**
 * created by: veli
 * date: 13/04/18 19:45
 */
@AndroidEntryPoint
class ContentBrowserActivity : Activity() {
    private val selectionViewModel: SharingSelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_browser)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val appBarLayout = findViewById<AppBarLayout>(R.id.app_bar)
        val toolbarDefaultBg = R.attr.backgroundTopBar.attrToRes(this).resToDrawable(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val navController = navController(R.id.nav_host_fragment)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            title = destination.label

            val bg = when (destination.id) {
                R.id.contentBrowserFragment, R.id.prepareIndexFragment  -> null
                else -> toolbarDefaultBg
            }
            if (Build.VERSION.SDK_INT < 16) {
                appBarLayout.setBackgroundDrawable(bg)
            } else {
                appBarLayout.background = bg
            }
        }
    }
}
