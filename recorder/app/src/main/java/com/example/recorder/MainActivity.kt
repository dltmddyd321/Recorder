package com.example.recorder

import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.recorder.State.AFTER_RECORDING
import com.example.recorder.State.BEFORE_RECORDING
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private val soundVisualizerView: SoundVisualizerView by lazy {
        findViewById(R.id.visual)
    }

    private val recordTimeTextView: CountUpView by lazy {
        findViewById(R.id.recordTimeTextView)
    }

    private val resetButton: Button by lazy {
        findViewById(R.id.resetButton)
    }

    private val recordButton: RecordButton by lazy {
        findViewById(R.id.recordBtn)
    }

    private val requiredPermissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)

    private val recordingFilePath: String by lazy {
        "${externalCacheDir?.absolutePath}/recording.3gp"
    } //녹음 파일 저장에 대한 Cache 경로 지정

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var state = BEFORE_RECORDING //기본 상태 지정
        set(value) { //상태에 따른 아이콘 이미지 변경을 위한 set 지정
            field = value
            resetButton.isEnabled = (value == State.AFTER_RECORDING) || (value == State.ON_PLAYING)
            recordButton.updateIconWithState(value)
            //새로운 값이 할당될때마다 함수에 반영되어 아이콘이 자동 변경
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestAudioPermission()
        initViews()
        bindViews()
        initVariables()
    }

    override fun onRequestPermissionsResult( //요청한 권한에 대한 결과값을 반환
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val audioRecordPermissionGranted =
            requestCode == REQUEST_RECORD_AUDIO_PERMISSION &&
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        //2가지 조건이 참이 되어 권한 부여

        if(!audioRecordPermissionGranted) {
            finish()
            //권한을 부여받지 못했다면 앱 종료
       }
    }

    private fun requestAudioPermission() {
        requestPermissions(requiredPermissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    private fun initViews() {

        recordButton.updateIconWithState(state)
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) //마이크에 접근
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) //출력 형태 지정
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) //인코더 시스템 지정
            setOutputFile(recordingFilePath) //녹음된 파일 관리 방식에 대한 지정
            prepare() //녹음을 할 수 있는 준비 상태 선언
        }
        recorder?.start()
        soundVisualizerView.startVisualizing(false)
        //레코딩 중엔 리플레이가 아니므로 false 전달
        recordTimeTextView.startCountUp()
        state = State.ON_RECORDING
        //녹음 시작
    }

    private fun stopRecording() {
        recorder?.run {
            stop()
            release()
        }
        recorder = null
        soundVisualizerView.stopVisualizing()
        recordTimeTextView.stopCountUp()
        state = State.AFTER_RECORDING
    } //녹음 정지

    private fun startPlaying() {
        player = MediaPlayer()
            .apply {
                setDataSource(recordingFilePath)
                prepare()
            }
        player?.setOnCompletionListener {
            //입력받은 파일을 전부 재생했을 때의 처리
            stopPlaying()
            state = AFTER_RECORDING
        }
        player?.start()
        soundVisualizerView.startVisualizing(true)
        recordTimeTextView.startCountUp()
        state = State.ON_PLAYING
        //녹음된 파일을 Cache 저장 경로를 통해 불러와 재생
    }

    private fun stopPlaying(){
        player?.release()
        player = null
        soundVisualizerView.stopVisualizing()
        recordTimeTextView.stopCountUp()
        state = State.AFTER_RECORDING
        //녹음 파일 재생 종료
    }

    private fun bindViews() {
        //상태별 애플리케이션 동작에 대한 함수를 지정하여 관리의 용이성을 높임
        soundVisualizerView.onRequestCurrentAmplitude = {
            recorder?.maxAmplitude ?: 0
        } //현재 사운드의 maxAmp 값을 반환
        resetButton.setOnClickListener { //Reset 버튼 구현
            stopPlaying()
            soundVisualizerView.clearVisualization()
            //시각화 초기화
            recordTimeTextView.clearCountTime()
            //시간 기록 초기화
            state = State.BEFORE_RECORDING
        }
        recordButton.setOnClickListener {
            when(state) {
                State.AFTER_RECORDING -> {
                    startRecording()
                }
                State.ON_RECORDING -> {
                    stopRecording()
                }
                State.AFTER_RECORDING -> {
                    startPlaying()
                }
                State.ON_PLAYING -> {
                    stopPlaying()
                }
            }
        }
    }

    private fun initVariables() {
        //Reset 버튼 활성화 기능 구현
        state = State.BEFORE_RECORDING
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 201
        //권한 요청을 위한 기본 값 지정
    }

}