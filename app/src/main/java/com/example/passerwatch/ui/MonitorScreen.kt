package com.example.passerwatch.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.passerwatch.BuildConfig
import com.example.passerwatch.camera.CameraController
import com.example.passerwatch.camera.FrameAnalyzer
import com.example.passerwatch.model.SnapshotRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MonitorScreen(vm: MonitorViewModel = viewModel()) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val rootView = LocalView.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var retry by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<SnapshotRecord?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<CameraController?>(null) }
    fun hasCamera() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    fun hasGalleryPermission() = Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(hasCamera()) }
    var galleryGranted by remember { mutableStateOf(hasGalleryPermission()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
        if (!allowed) vm.cameraError("需要相机权限才能检测，请在应用设置中授权。")
    }
    val galleryPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { galleryGranted = it }
    fun navigate(destination: Int) {
        if (destination != tab) { vm.stop(); selected = null; tab = destination }
        if (destination == 1) vm.reloadGallery()
    }
    fun openPhoto(record: SnapshotRecord) { vm.stop(); selected = record }
    DisposableEffect(owner) {
        fun refresh() { granted = hasCamera(); galleryGranted = hasGalleryPermission() }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { refresh(); vm.screenStarted() }
                Lifecycle.Event.ON_STOP -> { rootView.keepScreenOn = false; vm.screenStopped() }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) { refresh(); vm.screenStarted() }
        onDispose { rootView.keepScreenOn = false; owner.lifecycle.removeObserver(observer); vm.screenStopped() }
    }
    DisposableEffect(ui.running, rootView) {
        rootView.keepScreenOn = ui.running
        onDispose { rootView.keepScreenOn = false }
    }
    val preview = remember(context, retry, ui.settings) { PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FIT_CENTER
    } }
    DisposableEffect(granted, owner, preview, tab) {
        if (granted && tab == 0) {
            val analyzer = FrameAnalyzer(context.applicationContext, vm::sessionId, vm::onFrame, vm::analysisError,
                settings = ui.settings) { path, summary ->
                ContextCompat.getMainExecutor(context).execute {
                    vm.stop()
                    vm.photoError("模型输入：$summary")
                    selected = SnapshotRecord(path, System.currentTimeMillis(), File(path).length())
                }
            }
            val camera = CameraController(context, analyzer, vm::cameraReady, vm::cameraError,
                vm::photoCaptured, vm::photoError, vm::sessionId,
                vm::lensesAvailable, vm::zoomChanged, vm::lensEvidenceChanged, vm::cameraSwitchStarted, vm::digitalZoomChanged)
            controller = camera
            vm.photoRequester(camera::takePhoto)
            camera.bind(owner, preview)
            onDispose { vm.photoRequester(null); controller = null; camera.close() }
        } else onDispose { }
    }
    BackHandler(selected == null && tab != 0) { navigate(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SentryHeader(ui.running, tab) { showAbout = true } },
        bottomBar = { SentryNavigation(tab, ::navigate) }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (!galleryGranted && tab == 0) Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.padding(12.dp)) {
                    Text("Android 9 及以下需要相册保存权限")
                    TextButton(onClick = { galleryPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }) { Text("允许保存到相册") }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> LiveSentryPanel(ui, storage, preview = {
                        if (granted) key(preview) {
                            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
                                val height = minOf(maxHeight, maxWidth / ui.previewAspect)
                                AndroidView(factory = { preview }, modifier = Modifier.width(height * ui.previewAspect).height(height)
                                    .graphicsLayer { scaleX = ui.digitalCropRatio; scaleY = ui.digitalCropRatio })
                            }
                        }
                        else Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("先允许相机权限", style = MaterialTheme.typography.titleMedium)
                                Text("摆好手机后开始实时检测", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("授予相机权限") }
                                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}"))) }) { Text("打开应用设置") }
                            }
                        }
                    }, canStart = granted && galleryGranted && ui.cameraReady && storage.initialized,
                        manualEnabled = BuildConfig.DEBUG && !ui.running && galleryGranted && ui.cameraReady && storage.initialized && !storage.paused && storage.pending == 0,
                        onToggle = { if (ui.running) vm.stop() else vm.start() },
                        onRetry = { vm.stop(); vm.clearError(); retry++ },
                        onManual = { controller?.requestManualFrame() },
                        onOpenLatest = { storage.latest?.let(::openPhoto) }, onResumeSaving = vm::resumeSaving,
                        cameraControls = {
                            Column {
                                LensZoomControls(ui) { controller?.setZoom(it) }
                                if (BuildConfig.DEBUG) TextButton(onClick = { controller?.requestDiagnosticFrame() },
                                    enabled = ui.running && ui.cameraReady) { Text("调试：查看模型输入") }
                            }
                        })
                    1 -> SnapshotGalleryPanel(storage, records, vm::reloadGallery, ::openPhoto)
                    2 -> DetectionRulesPanel(ui.settings, ui.running, ui, storage.pending, vm::applySettings)
                }
            }
        }
    }
    selected?.let { record ->
        Dialog(onDismissRequest = { selected = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("现场原图", style = MaterialTheme.typography.titleLarge)
                    LocalCaptureImage(record.path, full = true)
                    Text(File(record.path).name, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text("${record.bytes / 1024} KB · App 内原文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { scope.launch {
                            try {
                                val uri = vm.galleryUri(record)
                                if (uri == null) Toast.makeText(context, "这张图片只有 App 内副本，或相册中的文件已移除", Toast.LENGTH_LONG).show()
                                else context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "image/jpeg")
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } catch (e: CancellationException) { throw e
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "没有可用的相册应用", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "无法打开系统相册图片，请检查文件或相册权限；App 内原图仍可查看", Toast.LENGTH_LONG).show()
                            }
                        } }) { Text("在系统相册打开") }
                        TextButton(onClick = { selected = null }) { Text("关闭") }
                    }
                }
            }
        }
    }
    if (showAbout) AlertDialog(onDismissRequest = { showAbout = false },
        title = { Text("照面 · 本机运行") },
        text = { Text("识别在手机本地完成。请保持应用前台；切换相册或规则页会停止检测，回到实时页后手动开始。\n\n新抓拍保存到 Pictures/Zhaomian，并保留 App 内副本。查看原图也会暂停检测。") },
        confirmButton = { TextButton(onClick = { showAbout = false }) { Text("知道了") } },
        dismissButton = { TextButton(onClick = { showAbout = false; context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"))) }) { Text("权限设置") } })
}

@Composable
private fun SentryHeader(running: Boolean, tab: Int, onAbout: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(if (tab == 0) "照面" else if (tab == 1) "抓拍相册" else "检测规则",
            modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(if (running) "监控中" else "本机离线", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall)
        IconButton(onClick = onAbout) { Text("ⓘ", fontSize = 22.sp) }
    }
}
@Composable
private fun SentryNavigation(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp)) {
            listOf("实时哨兵", "抓拍相册", "规则调优").forEachIndexed { index, label ->
                val color = if (index == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(Modifier.weight(1f).clickable { onSelect(index) }.heightIn(min = 56.dp).padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Canvas(Modifier.size(24.dp)) {
                        val stroke = Stroke(1.8.dp.toPx())
                        val w = size.width; val h = size.height
                        when (index) {
                            0 -> {
                                drawRoundRect(color, Offset(w*.1f,h*.24f), Size(w*.53f,h*.52f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
                                drawLine(color, Offset(w*.63f,h*.38f), Offset(w*.9f,h*.25f), stroke.width)
                                drawLine(color, Offset(w*.9f,h*.25f), Offset(w*.9f,h*.75f), stroke.width)
                                drawLine(color, Offset(w*.9f,h*.75f), Offset(w*.63f,h*.62f), stroke.width)
                            }
                            1 -> {
                                drawRect(color, Offset(w*.15f,h*.15f), Size(w*.7f,h*.7f), style = stroke)
                                drawCircle(color, w*.08f, Offset(w*.64f,h*.36f))
                                drawLine(color, Offset(w*.22f,h*.72f), Offset(w*.42f,h*.46f), stroke.width)
                                drawLine(color, Offset(w*.42f,h*.46f), Offset(w*.7f,h*.75f), stroke.width)
                            }
                            else -> {
                                for (n in 0..2) { val y = h*(.25f+.25f*n); drawLine(color, Offset(w*.1f,y), Offset(w*.9f,y), stroke.width)
                                    drawRect(color, Offset(w*(if(n==1).6f else .3f),y-h*.08f), Size(w*.12f,h*.16f)) }
                            }
                        }
                    }
                    Text(label, color = color, fontSize = 11.sp, fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

