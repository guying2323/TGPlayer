package com.tgplayer.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.tgplayer.android.data.AuthState
import com.tgplayer.android.data.Td
import com.tgplayer.android.ui.common.pressScale
import com.tgplayer.android.ui.theme.TmSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignInScreen(auth: AuthState) {
    val scope = rememberCoroutineScope()
    // The QR state can flip into the phone form without leaving the state, which is
    // why the toggle lives here and not inside one branch.
    var phoneMode by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canCancelAccount = Td.canCancelAccount()
    BackHandler(enabled = canCancelAccount) { Td.cancelAccountSwitch() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = TmSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (canCancelAccount) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { Td.cancelAccountSwitch() }) {
                    Text("← 返回上一个账号", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(4.dp))
        } else {
            Spacer(Modifier.height(48.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp)))
            Spacer(Modifier.size(10.dp))
            Text("TGPlayer", style = MaterialTheme.typography.headlineSmall)
        }
        Text("你的 Telegram 视频，边下边播", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(28.dp))

        when (auth) {
            is AuthState.Qr -> {
                if (phoneMode) {
                    PhoneForm(
                        phone = phone,
                        onPhoneChange = { phone = it },
                        hint = null,
                        submitLabel = "发送验证码",
                        onSubmit = { scope.launch { error = Td.submitPhone(phone.trim()) } },
                        switchLabel = "返回扫码登录",
                        onSwitch = { phoneMode = false },
                    )
                } else {
                    QrCard(auth.link)
                    Spacer(Modifier.height(12.dp))
                    Text("用手机 Telegram 扫描二维码登录", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        Text("  或  ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { phoneMode = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                    ) { Text("手机号 / 验证码登录", style = MaterialTheme.typography.labelLarge) }
                    Text(
                        "手机号登录同为一级入口，不藏进菜单",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            is AuthState.Phone -> PhoneForm(
                phone = phone,
                onPhoneChange = { phone = it },
                hint = if (auth.wrong) "号码似乎不对，请检查国家码" else null,
                submitLabel = "发送验证码",
                onSubmit = { scope.launch { error = Td.submitPhone(phone.trim()) } },
                switchLabel = "改用扫码登录",
                onSwitch = { scope.launch { error = Td.submitQr() } },
            )

            is AuthState.Code -> {
                Text("验证码已发送", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("请查看 Telegram 或短信", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("验证码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { scope.launch { error = Td.submitCode(code.trim()) } },
                    enabled = code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("下一步") }
            }

            is AuthState.Password -> {
                Text("两步验证", style = MaterialTheme.typography.titleMedium)
                if (!auth.hint.isNullOrBlank()) {
                    Text("提示：${auth.hint}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { scope.launch { error = Td.submitPassword(password) } },
                    enabled = password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("登录") }
            }

            is AuthState.Failed -> {
                Text(auth.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { Td.retry() }) { Text("重试") }
            }

            else -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("正在连接 Telegram…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            "非官方客户端 · GPL-3.0-or-later",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun PhoneForm(
    phone: String,
    onPhoneChange: (String) -> Unit,
    hint: String?,
    submitLabel: String,
    onSubmit: () -> Unit,
    switchLabel: String,
    onSwitch: () -> Unit,
) {
    Text("手机号登录", style = MaterialTheme.typography.titleMedium)
    Text(
        "输入带国家码的手机号，Telegram 会发验证码",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp),
    )
    if (hint != null) {
        Text(hint, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
    }
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("手机号，含国家码") },
        placeholder = { Text("+86 138…") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onSubmit,
        enabled = phone.isNotBlank(),
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(interaction),
    ) { Text(submitLabel) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onSwitch,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) { Text(switchLabel) }
}

@Composable
private fun QrCard(link: String) {
    var bitmap by remember(link) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(link) {
        bitmap = withContext(Dispatchers.Default) { runCatching { qrBitmap(link) }.getOrNull() }
    }
    Box(
        modifier = Modifier
            .size(236.dp)
            .background(Color(0xFFF4F6F8), RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "登录二维码", contentScale = ContentScale.Fit)
        } else {
            CircularProgressIndicator(color = Color(0xFF0B0D10))
        }
    }
}

private fun qrBitmap(text: String, size: Int = 560): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) 0xFF0B0D10.toInt() else 0xFFF4F6F8.toInt()
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}
