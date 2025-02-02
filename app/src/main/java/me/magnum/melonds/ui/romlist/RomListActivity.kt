package me.magnum.melonds.ui.romlist

import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.R
import me.magnum.melonds.common.contracts.DirectoryPickerContract
import me.magnum.melonds.databinding.ActivityRomListBinding
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.Rom
import me.magnum.melonds.domain.model.SortingMode
import me.magnum.melonds.ui.emulator.EmulatorActivity
import me.magnum.melonds.ui.settings.SettingsActivity
import me.magnum.melonds.utils.ConfigurationUtils

@AndroidEntryPoint
class RomListActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 1

        private const val FRAGMENT_ROM_LIST = "ROM_LIST"
        private const val FRAGMENT_NO_ROM_DIRECTORIES = "NO_ROM_DIRECTORY"
    }

    private val viewModel: RomListViewModel by viewModels()

    private val dsBiosPickerLauncher by lazy { registerForActivityResult(DirectoryPickerContract()) { uri ->
        if (uri != null) {
            viewModel.setDsBiosDirectory(uri)
            selectedRom?.let {
                loadRom(it)
            } ?: selectedFirmwareConsole?.let {
                bootFirmware(it)
            } ?: checkConfigDirectorySetup()
        }
    } }
    private val dsiBiosPickerLauncher by lazy { registerForActivityResult(DirectoryPickerContract()) { uri ->
        if (uri != null) {
            viewModel.setDsiBiosDirectory(uri)
            selectedRom?.let {
                loadRom(it)
            } ?: selectedFirmwareConsole?.let {
                bootFirmware(it)
            }
        }
    } }

    private var selectedRom: Rom? = null
    private var selectedFirmwareConsole: ConsoleType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        val binding = ActivityRomListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.getRomScanningDirectories().observe(this, Observer {
            if (it.isEmpty())
                addNoSearchDirectoriesFragment()
            else
                addRomListFragment()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menu == null)
            return super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.rom_list_menu, menu)

        val searchItem =  menu.findItem(R.id.action_search_roms)
        ContextCompat.getSystemService(this, SearchManager::class.java)?.let { searchManager ->
            val searchView = searchItem.actionView as SearchView
            searchView.apply {
                queryHint = getString(R.string.hint_search_roms)
                setSearchableInfo(searchManager.getSearchableInfo(componentName))
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setRomSearchQuery(newText)
                        return true
                    }
                })
            }
        }

        // Fix for action items not appearing after closing the search view
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                invalidateOptionsMenu()
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort_alphabetically -> {
                viewModel.setRomSorting(SortingMode.ALPHABETICALLY)
                return true
            }
            R.id.action_sort_recent -> {
                viewModel.setRomSorting(SortingMode.RECENTLY_PLAYED)
                return true
            }
            R.id.action_boot_firmware_ds -> {
                bootFirmware(ConsoleType.DS)
                return true
            }
            R.id.action_boot_firmware_dsi -> {
                bootFirmware(ConsoleType.DSi)
                return true
            }
            R.id.action_rom_list_refresh -> {
                viewModel.refreshRoms()
                return true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_STORAGE_PERMISSION)
            return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectedRom?.let {
                loadRom(it)
            }
        } else
            Toast.makeText(this, getString(R.string.info_no_storage_permission), Toast.LENGTH_LONG).show()
    }

    private fun checkConfigDirectorySetup(): Boolean {
        when (viewModel.getDsConfigurationDirStatus()) {
            ConfigurationUtils.ConfigurationDirStatus.VALID -> return true
            ConfigurationUtils.ConfigurationDirStatus.UNSET -> AlertDialog.Builder(this)
                    .setTitle(R.string.ds_bios_dir_not_set)
                    .setMessage(R.string.ds_bios_dir_not_set_info)
                    .setPositiveButton(R.string.ok) { _, _ -> dsBiosPickerLauncher.launch(null) }
                    .setCancelable(false)
                    .show()
            ConfigurationUtils.ConfigurationDirStatus.INVALID -> AlertDialog.Builder(this)
                    .setTitle(R.string.incorrect_bios_dir)
                    .setMessage(R.string.ds_incorrect_bios_dir_info)
                    .setPositiveButton(R.string.ok) { _, _ -> dsBiosPickerLauncher.launch(null) }
                    .setCancelable(false)
                    .show()
        }
        return false
    }

    private fun addNoSearchDirectoriesFragment() {
        var noRomDirectoriesFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_NO_ROM_DIRECTORIES) as NoRomSearchDirectoriesFragment?
        if (noRomDirectoriesFragment == null) {
            noRomDirectoriesFragment = NoRomSearchDirectoriesFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.layout_main, noRomDirectoriesFragment, FRAGMENT_NO_ROM_DIRECTORIES)
                    .commit()
        }
    }

    private fun addRomListFragment() {
        var romListFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_ROM_LIST) as RomListFragment?
        if (romListFragment == null) {
            romListFragment = RomListFragment.newInstance(true)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.layout_main, romListFragment, FRAGMENT_ROM_LIST)
                    .commit()
        }
        romListFragment.setRomSelectedListener { rom -> loadRom(rom) }
    }

    private fun loadRom(rom: Rom) {
        selectedRom = rom
        selectedFirmwareConsole = null

        val configurationDirStatus = viewModel.getRomConfigurationDirStatus(rom)
        if (configurationDirStatus == ConfigurationUtils.ConfigurationDirStatus.VALID) {
            val intent = EmulatorActivity.getRomEmulatorActivityIntent(this, rom)
            startActivity(intent)
        } else {
            showIncorrectConfigurationDirectoryDialog(configurationDirStatus, viewModel.getRomTargetConsoleType(rom))
        }
    }

    private fun bootFirmware(consoleType: ConsoleType) {
        selectedRom = null
        selectedFirmwareConsole = consoleType

        val configurationDirStatus = viewModel.getConsoleConfigurationDirStatus(consoleType)
        if (configurationDirStatus == ConfigurationUtils.ConfigurationDirStatus.VALID) {
            val intent = EmulatorActivity.getFirmwareEmulatorActivityIntent(this, consoleType)
            startActivity(intent)
        } else {
            showIncorrectConfigurationDirectoryDialog(configurationDirStatus, consoleType)
        }
    }

    private fun showIncorrectConfigurationDirectoryDialog(configurationDirStatus: ConfigurationUtils.ConfigurationDirStatus, consoleType: ConsoleType) {
        when (consoleType) {
            ConsoleType.DS -> {
                val titleRes: Int
                val messageRes: Int
                if (configurationDirStatus == ConfigurationUtils.ConfigurationDirStatus.UNSET) {
                    titleRes = R.string.ds_bios_dir_not_set
                    messageRes = R.string.ds_bios_dir_not_set_info
                } else {
                    titleRes = R.string.incorrect_bios_dir
                    messageRes = R.string.ds_incorrect_bios_dir_info
                }

                AlertDialog.Builder(this)
                        .setTitle(titleRes)
                        .setMessage(messageRes)
                        .setPositiveButton(R.string.ok) { _, _ -> dsBiosPickerLauncher.launch(null) }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
            }
            ConsoleType.DSi -> {
                val titleRes: Int
                val messageRes: Int
                if (configurationDirStatus == ConfigurationUtils.ConfigurationDirStatus.UNSET) {
                    titleRes = R.string.dsi_bios_dir_not_set
                    messageRes = R.string.dsi_bios_dir_not_set_info
                } else {
                    titleRes = R.string.incorrect_bios_dir
                    messageRes = R.string.dsi_incorrect_bios_dir_info
                }

                AlertDialog.Builder(this)
                        .setTitle(titleRes)
                        .setMessage(messageRes)
                        .setPositiveButton(R.string.ok) { _, _ -> dsiBiosPickerLauncher.launch(null) }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
            }
        }
    }
}