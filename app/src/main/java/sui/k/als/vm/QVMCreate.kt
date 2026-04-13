package sui.k.als.vm

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import org.json.JSONObject
import sui.k.als.R
import sui.k.als.localAppFont
import java.io.DataOutputStream

@Composable
fun VMCreate(configuration: VMConfig? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val stateMap = remember {
        mutableStateMapOf<String, Any>().apply {
            val raw = configuration?.raw
            val defaults = mapOf(
                "name" to "Ubuntu",
                "nice" to "-20",
                "smp" to "$(nproc)",
                "sockets" to "1",
                "cores" to "$(nproc)",
                "threads" to "1",
                "mem" to "6",
                "unit" to "G",
                "swiotlb" to "64M",
                "cd_enabled" to false,
                "cd_path" to "",
                "cd_boot" to "1",
                "path" to "",
                "cache" to "unsafe",
                "aio" to "threads",
                "discard" to "unmap",
                "queues" to "$(nproc)",
                "io_optimization" to true,
                "gpu" to true,
                "network" to true,
                "network_protocol" to "tcp",
                "network_port" to "2222-:22",
                "audio" to true,
                "p2" to "15",
                "p3" to "15",
                "vnc_enable" to false,
                "vnc_port" to "5900"
            )
            defaults.forEach { (k, v) ->
                put(k, if (raw != null && raw.has(k)) raw.get(k) else v)
            }
            if (configuration != null) put("name", configuration.name)
        }
    }
    val commandBuilder by remember {
        derivedStateOf {
            buildString {
                val name = stateMap["name"]
                val qmp = "-qmp unix:/data/local/tmp/als/dev/$name.sock,server,nowait "
                val vncPortStr = stateMap["vnc_port"].toString()
                val vncDisplay = try {
                    (vncPortStr.toInt() - 5900).coerceAtLeast(0)
                } catch (_: Exception) {
                    0
                }
                if (stateMap["vnc_enable"] as Boolean) append("(sleep 12 && am start -a android.intent.action.VIEW -d \"vnc://localhost:$vncPortStr\" com.realvnc.viewer.android) & ")
                append($$"LD_LIBRARY_PATH=$DIR/libs nice -n $${stateMap["nice"]} taskset $(printf '%x' $(( (1 << $(nproc)) - 1 ))) $DIR/qemu-system-aarch64 -L $DIR/pc-bios -M virt,confidential-guest-support=prot0 -accel gunyah -cpu host -smp $${stateMap["smp"]},sockets=$${stateMap["sockets"]},cores=$${stateMap["cores"]},threads=$${stateMap["threads"]} -m $${stateMap["mem"]}$${stateMap["unit"]} -object arm-confidential-guest,id=prot0,swiotlb-size=$${stateMap["swiotlb"]} -bios $DIR/QEMU_EFI.fd ")
                if (stateMap["io_optimization"] as Boolean) append("-object iothread,id=io0 ")
                if (stateMap["cd_enabled"] as Boolean && (stateMap["cd_path"] as String).isNotEmpty()) append(
                    "-drive file=${stateMap["cd_path"]},if=none,id=dr1,format=raw,aio=threads,media=cdrom -device virtio-blk-pci,drive=dr1,bootindex=${stateMap["cd_boot"]} "
                )
                append("-drive file=${(stateMap["path"] as String).ifEmpty { "/data/local/tmp/als/resolute-desktop-arm64.rw" }},if=none,id=dr0,cache=${stateMap["cache"]},aio=${stateMap["aio"]},discard=${stateMap["discard"]} -device virtio-blk-pci,drive=dr0,num-queues=${stateMap["queues"]}${if (stateMap["io_optimization"] as Boolean) ",iothread=io0" else ""},disable-legacy=on,disable-modern=off,bootindex=${if (stateMap["cd_enabled"] as Boolean) "2" else "1"} ")
                if (stateMap["network"] as Boolean) append("-netdev user,id=net0,hostfwd=${stateMap["network_protocol"]}::${stateMap["network_port"]} -device virtio-net-pci,netdev=net0,disable-legacy=on,disable-modern=off ")
                if (stateMap["audio"] as Boolean) append("-audiodev aaudio,id=snd0 -device virtio-sound-pci,audiodev=snd0,disable-legacy=on,disable-modern=off ")
                if (stateMap["gpu"] as Boolean) {
                    append("-device virtio-gpu-pci,disable-legacy=on,disable-modern=off ")
                    if (stateMap["vnc_enable"] as Boolean) append("-vnc :$vncDisplay ")
                }
                append("-device qemu-xhci,id=usb-bus,p2=${stateMap["p2"]},p3=${stateMap["p3"]} -device usb-tablet,bus=usb-bus.0 -device usb-kbd,bus=usb-bus.0 $qmp-serial stdio")
            }
        }
    }
    val tabs = listOf(
        R.string.archive,
        R.string.processor,
        R.string.memory,
        R.string.cdrom,
        R.string.disk,
        R.string.display,
        R.string.network,
        R.string.audio,
        R.string.usb,
        R.string.preview
    ).map { stringResource(it) }
    ExpressiveCanvas(
        title = stringResource(R.string.add_virtual_machine), navigationItems = tabs, onAction = {
            try {
                val output = JSONObject().apply {
                    stateMap.forEach { (k, v) -> put(k, v) }; put(
                    "command", commandBuilder
                )
                }
                DataOutputStream(Runtime.getRuntime().exec("su").outputStream).use {
                    it.writeBytes(
                        "mkdir -p /data/local/tmp/als/dev\necho '${
                            output.toString().replace("'", "'\\''")
                        }' > /data/local/tmp/als/dev/${stateMap["name"]}\nexit\n"
                    )
                    it.flush()
                }
                onBack()
            } catch (_: Exception) {
            }
        }) { index ->
        val appFont = localAppFont.current
        when (index) {
            0 -> ListCellGroup(
                listOf(
                    R.string.configuration_name to "name", R.string.nice_value to "nice"
                ), stateMap
            )

            1 -> ListCellGroup(
                listOf(
                    R.string.smp_threads to "smp",
                    R.string.cpu_cores to "cores",
                    R.string.cpu_sockets to "sockets",
                    R.string.cpu_threads to "threads"
                ), stateMap
            )

            2 -> ListCellGroup(
                listOf(
                    R.string.memory_size to "mem", R.string.swiotlb_buffer_size to "swiotlb"
                ), stateMap
            )

            3 -> {
                ToggleCell(
                    stringResource(R.string.enable_cdrom), stateMap["cd_enabled"] as Boolean
                ) { stateMap["cd_enabled"] = it }
                if (stateMap["cd_enabled"] as Boolean) {
                    Separator()
                    Row(Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            val request =
                                DownloadManager.Request("https://cdimage.ubuntu.com/daily-live/current/resolute-desktop-arm64.iso".toUri())
                                    .setTitle("Ubuntu ISO")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS, "ubuntu-arm64.iso"
                                    )
                            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(
                                request
                            )
                        }
                        .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.download_ubuntu_iso),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = appFont,
                            color = Color(0xFFE95420)
                        )
                    }
                    Separator()
                    ListCellGroup(
                        listOf(
                            R.string.cdrom_path to "cd_path", R.string.boot_index to "cd_boot"
                        ), stateMap
                    )
                }
            }

            4 -> {
                ListCellGroup(
                    listOf(
                        R.string.disk_path to "path",
                        R.string.cache_mode to "cache",
                        R.string.aio_mode to "aio",
                        R.string.discard_mode to "discard",
                        R.string.queue_count to "queues"
                    ), stateMap
                )
                Separator()
                ToggleCell(
                    stringResource(R.string.io_thread_optimization),
                    stateMap["io_optimization"] as Boolean
                ) { stateMap["io_optimization"] = it }
            }

            5 -> {
                ToggleCell(
                    stringResource(R.string.display_output), stateMap["gpu"] as Boolean
                ) { stateMap["gpu"] = it }
                Separator()
                ToggleCell(
                    stringResource(R.string.auto_launch_vnc), stateMap["vnc_enable"] as Boolean
                ) { stateMap["vnc_enable"] = it }
                if (stateMap["vnc_enable"] as Boolean) {
                    Separator()
                    InputCell(
                        stringResource(R.string.vnc_port), stateMap["vnc_port"].toString()
                    ) { stateMap["vnc_port"] = it }
                }
            }

            6 -> {
                ToggleCell(
                    stringResource(R.string.network_device), stateMap["network"] as Boolean
                ) { stateMap["network"] = it }
                if (stateMap["network"] as Boolean) {
                    Separator()
                    ListCellGroup(
                        listOf(
                            R.string.network_protocol to "network_protocol",
                            R.string.port_forwarding to "network_port"
                        ), stateMap
                    )
                }
            }

            7 -> ToggleCell(
                stringResource(R.string.audio_output), stateMap["audio"] as Boolean
            ) { stateMap["audio"] = it }

            8 -> ListCellGroup(
                listOf(
                    R.string.usb_2_0_ports to "p2", R.string.usb_3_0_ports to "p3"
                ), stateMap
            )

            9 -> SelectionContainer {
                Text(
                    commandBuilder,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = appFont
                )
            }
        }
    }
}

@Composable
fun ListCellGroup(items: List<Pair<Int, String>>, stateMap: MutableMap<String, Any>) {
    items.forEachIndexed { i, p ->
        InputCell(stringResource(p.first), stateMap[p.second].toString()) {
            stateMap[p.second] = it
        }
        if (i < items.size - 1) Separator()
    }
}