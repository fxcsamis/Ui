package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.view.View
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

fun getWaveBallHtml(fontSizePx: String = "5.5vmin", transparentBg: Boolean = true): String {
    val bgCss = if (transparentBg) "transparent" else "#1a1a1a"
    return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>Wave Ball Loader</title>
<style>
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  html, body {
    margin: 0;
    padding: 0;
    width: 100vw;
    height: 100vh;
    background: $bgCss;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  /* From Uiverse.io by Praashoo7 */
  .main {
    width: 100vw;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: $fontSizePx;
  }

  .up {
    position: relative;
    width: 13em;
    height: 13em;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .loaders,
  .loadersB {
    position: absolute;
    top: 0;
    left: 0;
    width: 13em;
    height: 13em;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .loader {
    position: absolute;
    top: 0;
    left: 50%;
    margin-left: -0.575em;
    width: 1.15em;
    height: 13em;
    border-radius: 50px;
    background: #e0e0e0;
    --rot: 0deg;
    transform-origin: center center;
    animation: handPulse 2.6s ease-in-out infinite;
    will-change: transform, opacity;
    backface-visibility: hidden;
    box-shadow: 0 0 8px rgba(255, 255, 255, 0.35);
    transform: rotate(var(--rot));
  }
  .loader:after {
    content: "";
    position: absolute;
    left: 0;
    top: 0;
    width: 1.15em;
    height: 5em;
    background: #e0e0e0;
    border-radius: 50px;
    border: 1px solid #e2e2e2;
    box-shadow:
      inset 5px 5px 15px #d3d2d2ab,
      inset -5px -5px 15px #e9e9e9ab;
    mask-image: linear-gradient(
      to bottom,
      black calc(100% - 48px),
      transparent 100%
    );
  }
  .loader::before {
    content: "";
    position: absolute;
    bottom: 0;
    right: 0;
    width: 1.15em;
    height: 4.5em;
    background: #e0e0e0;
    border-radius: 50px;
    border: 1px solid #e2e2e2;
    box-shadow:
      inset 5px 5px 15px #d3d2d2ab,
      inset -5px -5px 15px #e9e9e9ab;
    mask-image: linear-gradient(
      to top,
      black calc(100% - 48px),
      transparent 100%
    );
  }
  .loaderA {
    position: absolute;
    top: 0;
    left: 50%;
    margin-left: -0.575em;
    width: 1.15em;
    height: 13em;
    border-radius: 50px;
    background: transparent;
    transform-origin: center center;
  }
  .ball0,
  .ball1,
  .ball2,
  .ball3,
  .ball4,
  .ball5,
  .ball6,
  .ball7,
  .ball8,
  .ball9 {
    position: absolute;
    width: 1.15em;
    height: 1.15em;
    box-shadow:
      rgba(0, 0, 0, 0.17) 0px -10px 10px 0px inset,
      rgba(0, 0, 0, 0.15) 0px -15px 15px 0px inset,
      rgba(0, 0, 0, 0.1) 0px -40px 20px 0px inset,
      rgba(0, 0, 0, 0.06) 0px 2px 1px,
      rgba(0, 0, 0, 0.09) 0px 4px 2px,
      rgba(0, 0, 0, 0.09) 0px 8px 4px,
      rgba(0, 0, 0, 0.09) 0px 16px 8px,
      rgba(0, 0, 0, 0.09) 0px 32px 16px,
      0px -1px 15px -8px rgba(0, 0, 0, 0.09);
    border-radius: 50%;
    background-color: rgb(232, 232, 232, 1);
    animation-name: dropIn, move;
    animation-duration: 1s, 3.63s;
    animation-timing-function: cubic-bezier(0.22, 1, 0.36, 1), ease-in-out;
    animation-iteration-count: 1, infinite;
    animation-fill-mode: both, none;
    will-change: transform, opacity;
    backface-visibility: hidden;
  }
  .loader:nth-child(1) {
    animation-delay: 0s;
  }
  .loader:nth-child(2) {
    --rot: 20deg;
    animation-delay: 0.22s;
  }
  .loader:nth-child(3) {
    --rot: 40deg;
    animation-delay: 0.44s;
  }
  .loader:nth-child(4) {
    --rot: 60deg;
    animation-delay: 0.66s;
  }
  .loader:nth-child(5) {
    --rot: 80deg;
    animation-delay: 0.88s;
  }
  .loader:nth-child(6) {
    --rot: 100deg;
    animation-delay: 1.1s;
  }
  .loader:nth-child(7) {
    --rot: 120deg;
    animation-delay: 1.32s;
  }
  .loader:nth-child(8) {
    --rot: 140deg;
    animation-delay: 1.54s;
  }
  .loader:nth-child(9) {
    --rot: 160deg;
    animation-delay: 1.76s;
  }

  .loaderA:nth-child(2) {
    transform: rotate(20deg);
  }
  .loaderA:nth-child(3) {
    transform: rotate(40deg);
  }
  .loaderA:nth-child(4) {
    transform: rotate(60deg);
  }
  .loaderA:nth-child(5) {
    transform: rotate(80deg);
  }
  .loaderA:nth-child(6) {
    transform: rotate(100deg);
  }
  .loaderA:nth-child(7) {
    transform: rotate(120deg);
  }
  .loaderA:nth-child(8) {
    transform: rotate(140deg);
  }
  .loaderA:nth-child(9) {
    transform: rotate(160deg);
  }

  /* each ball: first delay = when it drops in from inside the "hand",
     second delay = when its continuous loop wave takes over
     (timed to start right as the drop finishes, so there's no jump) */
  .ball0 {
    animation-delay: 0s, 1s;
  }
  .ball1 {
    animation-delay: 0.15s, 1.15s;
  }
  .ball2 {
    animation-delay: 0.3s, 1.3s;
  }
  .ball3 {
    animation-delay: 0.45s, 1.45s;
  }
  .ball4 {
    animation-delay: 0.6s, 1.6s;
  }
  .ball5 {
    animation-delay: 0.75s, 1.75s;
  }
  .ball6 {
    animation-delay: 0.9s, 1.9s;
  }
  .ball7 {
    animation-delay: 1.05s, 2.05s;
  }
  .ball8 {
    animation-delay: 1.2s, 2.2s;
  }

  /* balls start hidden, gathered near the base of the "hand" (top of the
     bar), then fall down and settle at the shared center point —
     smooth ease-out, no bounce/overshoot, to avoid any jitter */
  @keyframes dropIn {
    0% {
      transform: translateY(0em) scale(0.25);
      opacity: 0;
    }
    45% {
      opacity: 1;
    }
    100% {
      transform: translateY(6em) scale(1);
      opacity: 1;
    }
  }

  /* continuous wave loop, phased to start/end at the center point (6em)
     so it hands off seamlessly from dropIn with no visual jump */
  @keyframes move {
    0% {
      transform: translateY(6em) scale(1);
    }
    25% {
      transform: translateY(12em) scale(1);
    }
    75% {
      transform: translateY(0em) scale(1);
    }
    100% {
      transform: translateY(6em) scale(1);
    }
  }

  /* background "hand" bars breathe in and out like Claude's icon glow —
     only opacity + scale are animated (both compositor-only, no repaint),
     which keeps this smooth even on low-power devices */
  @keyframes handPulse {
    0%,
    100% {
      transform: rotate(var(--rot)) scale(1);
      opacity: 0.5;
    }
    50% {
      transform: rotate(var(--rot)) scale(1.06);
      opacity: 1;
    }
  }
</style>
</head>
<body>

<!-- From Uiverse.io by Praashoo7 -->
<div class="main">
  <div class="up">
    <div class="loaders">
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
      <div class="loader"></div>
    </div>
    <div class="loadersB">
      <div class="loaderA">
        <div class="ball0"></div>
      </div>
      <div class="loaderA">
        <div class="ball1"></div>
      </div>
      <div class="loaderA">
        <div class="ball2"></div>
      </div>
      <div class="loaderA">
        <div class="ball3"></div>
      </div>
      <div class="loaderA">
        <div class="ball4"></div>
      </div>
      <div class="loaderA">
        <div class="ball5"></div>
      </div>
      <div class="loaderA">
        <div class="ball6"></div>
      </div>
      <div class="loaderA">
        <div class="ball7"></div>
      </div>
      <div class="loaderA">
        <div class="ball8"></div>
      </div>
    </div>
  </div>
</div>

</body>
</html>
""".trimIndent()
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun WaveBallLoaderHtmlView(
    modifier: Modifier = Modifier,
    fontSizePx: String = "5.5vmin",
    transparentBg: Boolean = true
) {
    val htmlData = remember(fontSizePx, transparentBg) {
        getWaveBallHtml(fontSizePx, transparentBg)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false
                settings.minimumFontSize = 1
                settings.minimumLogicalFontSize = 1
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setOnTouchListener { _, _ -> false }
                loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

fun createWaveBallLoaderBitmap(context: Context, sizeDp: Int = 36): Bitmap {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt().coerceAtLeast(72)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = px / 2f
    val radius = px * 0.42f

    val paintBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E0E0E0")
        strokeWidth = px * 0.08f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    val paintBall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }

    val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0284C7")
        style = Paint.Style.FILL
    }

    for (i in 0 until 9) {
        val angle = i * 20f
        canvas.save()
        canvas.rotate(angle, center, center)
        canvas.drawLine(center, center - radius, center, center - radius * 0.35f, paintBar)
        canvas.restore()
    }

    for (i in 0 until 9) {
        val angle = (i * 20f) * Math.PI.toFloat() / 180f
        val ballDist = radius * (0.35f + 0.45f * Math.sin(i * 0.7).toFloat().let { (it + 1f) / 2f })
        val bx = center + ballDist * Math.sin(angle.toDouble()).toFloat()
        val by = center - ballDist * Math.cos(angle.toDouble()).toFloat()

        canvas.drawCircle(bx, by, px * 0.08f, paintGlow)
        canvas.drawCircle(bx, by, px * 0.05f, paintBall)
    }

    return bitmap
}
