package sui.k.als.vm
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sui.k.als.R
import sui.k.als.localAppFont
import java.io.DataOutputStream
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VMCreate(onBack: () -> Unit) {
    val font = localAppFont.current
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf(stringResource(R.string.tab_archive), stringResource(R.string.tab_processor), stringResource(R.string.tab_memory), stringResource(R.string.tab_disk), stringResource(R.string.tab_display), stringResource(R.string.tab_network), stringResource(R.string.tab_audio), stringResource(R.string.tab_usb), stringResource(R.string.tab_preview))
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val accentColor = Color(0xFFE95420)
    val customScheme = lightColorScheme(primary = accentColor, onPrimary = Color.White, primaryContainer = Color(0xFFFDEEE9), onPrimaryContainer = accentColor, surface = Color.White, onSurface = Color.Black)
    var configurationName by rememberSaveable { mutableStateOf("Ubuntu") }
    var priorityNiceValue by rememberSaveable { mutableStateOf("-20") }
    var cpuSmpThreads by rememberSaveable { mutableStateOf("$(nproc)") }
    var cpuSocketsCount by rememberSaveable { mutableStateOf("1") }
    var cpuCoresCount by rememberSaveable { mutableStateOf("$(nproc)") }
    var cpuThreadsPerCore by rememberSaveable { mutableStateOf("1") }
    var memoryNumericValue by rememberSaveable { mutableStateOf("6") }
    var memoryUnitSuffix by rememberSaveable { mutableStateOf("G") }
    var swiotlbBufferSize by rememberSaveable { mutableStateOf("64M") }
    var readWritePath by rememberSaveable { mutableStateOf("/data/local/tmp/als/resolute-desktop-arm64.rw") }
    var diskCacheMode by rememberSaveable { mutableStateOf("unsafe") }
    var diskAioMode by rememberSaveable { mutableStateOf("threads") }
    var diskDiscardMode by rememberSaveable { mutableStateOf("unmap") }
    var diskQueuesCount by rememberSaveable { mutableStateOf("$(nproc)") }
    var ioThreadOptimizationEnabled by rememberSaveable { mutableStateOf(true) }
    var gpuDisplayEnabled by rememberSaveable { mutableStateOf(true) }
    var networkEnabled by rememberSaveable { mutableStateOf(true) }
    var hostForwardProtocol by rememberSaveable { mutableStateOf("tcp") }
    var hostForwardPortMapping by rememberSaveable { mutableStateOf("2222-:22") }
    var audioEnabled by rememberSaveable { mutableStateOf(true) }
    var usbP2PortCount by rememberSaveable { mutableStateOf("15") }
    var usbP3PortCount by rememberSaveable { mutableStateOf("15") }
    val ioThreadObject = if (ioThreadOptimizationEnabled) "-object iothread,id=io0 " else ""
    val ioThreadDeviceParam = if (ioThreadOptimizationEnabled) ",iothread=io0" else ""
    val gpuCommand = if (gpuDisplayEnabled) "-device virtio-gpu-pci,disable-legacy=on,disable-modern=off " else ""
    val networkCommand = if (networkEnabled) "-netdev user,id=net0,hostfwd=$hostForwardProtocol::$hostForwardPortMapping -device virtio-net-pci,netdev=net0,disable-legacy=on,disable-modern=off " else ""
    val audioCommand = if (audioEnabled) "-audiodev aaudio,id=snd0 -device virtio-sound-pci,audiodev=snd0,disable-legacy=on,disable-modern=off " else ""
    val fullLaunchCommand = "LD_LIBRARY_PATH=\$DIR/libs nice -n $priorityNiceValue taskset \$(printf '%x' \$(( (1 << \$(nproc)) - 1 ))) \$DIR/qemu-system-aarch64 -L \$DIR/pc-bios -M virt,confidential-guest-support=prot0 -accel gunyah -cpu host -smp $cpuSmpThreads,sockets=$cpuSocketsCount,cores=$cpuCoresCount,threads=$cpuThreadsPerCore -m $memoryNumericValue$memoryUnitSuffix -object arm-confidential-guest,id=prot0,swiotlb-size=$swiotlbBufferSize -bios \$DIR/QEMU_EFI.fd $ioThreadObject-drive file=$readWritePath,if=none,id=dr0,cache=$diskCacheMode,aio=$diskAioMode,discard=$diskDiscardMode -device virtio-blk-pci,drive=dr0,num-queues=$diskQueuesCount$ioThreadDeviceParam,disable-legacy=on,disable-modern=off,bootindex=1 $networkCommand${audioCommand}${gpuCommand}-device qemu-xhci,id=usb-bus,p2=$usbP2PortCount,p3=$usbP3PortCount -device usb-tablet,bus=usb-bus.0 -device usb-kbd,bus=usb-bus.0 -serial stdio"
    fun saveConfigurationToRoot(jsonContent: String, fileName: String, onResult: (Boolean) -> Unit) {
        try {
            val runtimeProcess = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(runtimeProcess.outputStream)
            outputStream.writeBytes("mkdir -p /data/local/tmp/als/dev\n")
            outputStream.writeBytes("echo '${jsonContent.replace("'", "'\\''")}' > /data/local/tmp/als/dev/$fileName\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            val exitCode = runtimeProcess.waitFor()
            onResult(exitCode == 0)
        } catch (_: Exception) {
            onResult(false)
        }
    }
    MaterialTheme(colorScheme = customScheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(modifier = Modifier.background(Color.White)) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.label_add_vm), fontSize = 9.sp, fontFamily = font, modifier = Modifier.padding(horizontal = 9.dp, vertical = 16.dp), color = Color.Black)
                    SecondaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.White, edgePadding = 9.dp, divider = {}, indicator = {
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pagerState.currentPage), color = accentColor)
                    }) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(selected = pagerState.currentPage == index, onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }, text = { Text(title, fontFamily = font, fontSize = 9.sp) }, selectedContentColor = accentColor, unselectedContentColor = Color.Gray)
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    saveConfigurationToRoot("""{"name":"$configurationName","cmd":"$fullLaunchCommand"}""", configurationName) { success ->
                        if (success) onBack()
                    }
                }, containerColor = accentColor, shape = RoundedCornerShape(9.dp), modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.Save, null, tint = Color.White) }
            }
        ) { paddingValues ->
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().background(Color.White).padding(paddingValues), verticalAlignment = Alignment.Top) { pageIndex ->
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp).verticalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.padding(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        when (pageIndex) {
                            0 -> {
                                TextFieldFull(stringResource(R.string.field_config_name), configurationName, font) { configurationName = it }
                                TextFieldFull(stringResource(R.string.field_nice_value), priorityNiceValue, font) { priorityNiceValue = it }
                            }
                            1 -> {
                                TextFieldFull(stringResource(R.string.field_smp_threads), cpuSmpThreads, font) { cpuSmpThreads = it }
                                TextFieldFull(stringResource(R.string.field_cpu_cores), cpuCoresCount, font) { cpuCoresCount = it }
                                TextFieldFull(stringResource(R.string.field_cpu_sockets), cpuSocketsCount, font) { cpuSocketsCount = it }
                                TextFieldFull(stringResource(R.string.field_cpu_threads), cpuThreadsPerCore, font) { cpuThreadsPerCore = it }
                            }
                            2 -> {
                                TextFieldFull(stringResource(R.string.field_mem_value), memoryNumericValue, font) { memoryNumericValue = it }
                                TextFieldFull(stringResource(R.string.field_swiotlb), swiotlbBufferSize, font) { swiotlbBufferSize = it }
                            }
                            3 -> {
                                TextFieldFull(stringResource(R.string.field_disk_path), readWritePath, font) { readWritePath = it }
                                TextFieldFull(stringResource(R.string.field_disk_cache), diskCacheMode, font) { diskCacheMode = it }
                                TextFieldFull(stringResource(R.string.field_disk_aio), diskAioMode, font) { diskAioMode = it }
                                TextFieldFull(stringResource(R.string.field_disk_discard), diskDiscardMode, font) { diskDiscardMode = it }
                                TextFieldFull(stringResource(R.string.field_disk_queues), diskQueuesCount, font) { diskQueuesCount = it }
                                SwitchRowFull(stringResource(R.string.switch_io_thread), ioThreadOptimizationEnabled, font) { ioThreadOptimizationEnabled = it }
                            }
                            4 -> { SwitchRowFull(stringResource(R.string.switch_display), gpuDisplayEnabled, font) { gpuDisplayEnabled = it } }
                            5 -> {
                                SwitchRowFull(stringResource(R.string.switch_network), networkEnabled, font) { networkEnabled = it }
                                if (networkEnabled) {
                                    TextFieldFull(stringResource(R.string.field_net_protocol), hostForwardProtocol, font) { hostForwardProtocol = it }
                                    TextFieldFull(stringResource(R.string.field_net_port), hostForwardPortMapping, font) { hostForwardPortMapping = it }
                                }
                            }
                            6 -> { SwitchRowFull(stringResource(R.string.switch_audio), audioEnabled, font) { audioEnabled = it } }
                            7 -> {
                                TextFieldFull(stringResource(R.string.field_usb_p2), usbP2PortCount, font) { usbP2PortCount = it }
                                TextFieldFull(stringResource(R.string.field_usb_p3), usbP3PortCount, font) { usbP3PortCount = it }
                            }
                            8 -> {
                                Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF9F9F9), shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(stringResource(R.string.title_preview), fontSize = 9.sp, color = accentColor, fontFamily = font)
                                        Spacer(Modifier.height(12.dp))
                                        Text(text = fullLaunchCommand, fontSize = 9.sp, color = Color.Black, lineHeight = 14.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}
@Composable
fun TextFieldFull(labelText: String, valueText: String, fontFamily: FontFamily?, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = valueText, onValueChange = onValueChange, label = { Text(labelText, fontFamily = fontFamily, fontSize = 9.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 9.sp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE95420), focusedLabelColor = Color(0xFFE95420), unfocusedBorderColor = Color(0xFFE0E0E0)))
}
@Composable
fun SwitchRowFull(labelText: String, checkedState: Boolean, fontFamily: FontFamily?, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(labelText, fontFamily = fontFamily, fontSize = 9.sp, color = Color.Black)
        Switch(checked = checkedState, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE95420), uncheckedTrackColor = Color(0xFFEEEEEE), uncheckedBorderColor = Color.Transparent))
    }
}