package com.example.recorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class SoundVisualizerView(
    context: Context,
    attrs: AttributeSet? = null
): View(context, attrs){
    private val amplitudePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.purple_500)
        strokeWidth = LINE_WIDTH
        strokeCap = Paint.Cap.ROUND
    } //계단화 방지 플래그

    var onRequestCurrentAmplitude: (() -> Int)? = null

    private var drawingWidth: Int = 0
    private var drawingHeight: Int = 0
    private var drawingAmplitudes: List<Int> = emptyList()
    //(0..10).map { Random.nextInt(Short.MAX_VALUE.toInt()) }
    //32767 ~ 0 중 10개 값을 랜덤으로 출력한뒤 저장하는 리스트
    private var isReplaying: Boolean = false
    private var replayingPosition: Int = 0

    private val visualizeRepeatAction: Runnable = object : Runnable {
        override fun run() {
            if (!isReplaying) {


            val currentAmplitude = onRequestCurrentAmplitude?.invoke() ?: 0
            //정의된 함수를 통해 현재 Amp 값 반환

            drawingAmplitudes = listOf(currentAmplitude) + drawingAmplitudes
            } else {
                replayingPosition++
                //시각화 박스의 개수 증가
            }
            invalidate()
            //데이터 갱신

            handler?.postDelayed(this, ACTION_INTERNAL)
            //ACTION_INTERNAL 뒤에 다시 실행
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingWidth = w
        drawingHeight = h
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        val centerY = drawingHeight/2f
        var offsetX = drawingWidth.toFloat()

        drawingAmplitudes
            .let { amplitudes ->
                if(isReplaying) {
                    amplitudes.takeLast(replayingPosition)
                    //가장 뒤에 있는 것부터 순서대로 다시 가져옴
                } else {
                    amplitudes
                }
            }
            .forEach { amplitude ->
            val lineLength = amplitude / MAX_AMPLITUDE * drawingHeight * 0.8F
            //배열 속 각각의 음성 시각화 이미지 사이즈 지정

            offsetX -= LINE_SPACE
            if(offsetX < 0) return@forEach
            //View의 바깥으로 벗어날때 예외처리

            canvas.drawLine(
                offsetX,
                centerY - lineLength / 2F,
                offsetX,
                centerY + lineLength / 2F,
                amplitudePaint
            )
        }
    }

    fun startVisualizing(isReplaying: Boolean) {
        this.isReplaying = isReplaying
        //다시 재생에 대한 상태 변수 이용
        handler?.post(visualizeRepeatAction)
    } //함수가 호출되면 핸들러 기능이 계속 반복 실행

    fun stopVisualizing() {
        replayingPosition = 0
        handler?.removeCallbacks(visualizeRepeatAction)
    } //반복 실행 제거

    fun clearVisualization() {
        drawingAmplitudes = emptyList()
        invalidate()
    }

    companion object {
        private const val LINE_WIDTH = 10F
        private const val LINE_SPACE = 15F
        private const val MAX_AMPLITUDE = Short.MAX_VALUE
        private const val ACTION_INTERNAL = 20L
    } //시각화에 대한 형태 지정
}

