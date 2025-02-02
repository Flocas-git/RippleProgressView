package com.example.rippleprogressview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var rippleProgressView: RippleProgressView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rippleProgressView = findViewById(R.id.rippleProgressView)
        rippleProgressView.setProgress(60f) // ✅ 设置初始进度为60%
        rippleProgressView.startAnimation() // ✅ 启动波纹动画
    }

    override fun onDestroy() {
        super.onDestroy()
        rippleProgressView.stopAnimation() // ✅ 停止所有动画
    }
}