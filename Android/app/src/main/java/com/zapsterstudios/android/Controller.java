package com.zapsterstudios.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Button;
import android.widget.SeekBar;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.UUID;

public class Controller extends AppCompatActivity {

    // 블루투스용 전역 변수지정
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    // 블루투스 SPP(Serial Port Profile) UUID 고유번호(전세게 공통)
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // UI용 전역 변수지정
    private SeekBar movementSpeed;

    private Button btnForward;
    private Button btnBackwards;
    private Button btnTurnLeft;
    private Button btnTurnRight;

    private Button btnUp;
    private Button btnDown;
    private Button btnRotateLeft;
    private Button btnRotateRight;

    final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 클래스 생성 시 자동 실행
        super.onCreate(savedInstanceState);

        // 주소 검색
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS);

        // UI 조정기 화면출력
        setContentView(R.layout.activity_controller);

        // 블루투스에 연결
        new ConnectBT().execute();

        // 스레드로 신호 활성화
        final Handler h = new Handler();
        h.postDelayed(new Runnable()
        {
            private long time = 0;

            @Override
            public void run()
            {
                time += 1000;
                sendBluetoothSignal("X");
                h.postDelayed(this, 1000);
            }
        }, 1000);

        // 탐색바 - 이동 속도(저속-고속)
        movementSpeed = (SeekBar) findViewById(R.id.movementSpeed);
        movementSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendBluetoothSignal(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 버튼 - 전진(피치조정-엘리베이터레버 뒤고속)
        btnForward = (Button) findViewById(R.id.btnForward);
        btnForward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "W", "w");
                return false;
            }
        });

        // 버튼 - 후진(피치조정-엘리베이터레버 앞고속)
        btnBackwards = (Button) findViewById(R.id.btnBackwards);
        btnBackwards.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "S", "s");
                return false;
            }
        });

        // 버튼 - 좌측이동(롤조정-에일러론Aileron레버 우고속)
        btnTurnLeft = (Button) findViewById(R.id.btnTurnLeft);
        btnTurnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "A", "a");
                return false;
            }
        });

        // 버튼 - 우측이동(롤조정-에일러론Aileron레버 좌고속)
        btnTurnRight = (Button) findViewById(R.id.btnTurnRight);
        btnTurnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "D", "d");
                return false;
            }
        });

        // 버튼 - 상승(피치조정-스로틀Throttle레버 모두 고속)
        btnUp = (Button) findViewById(R.id.btnUp);
        btnUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "T", "t");
                return false;
            }
        });

        // 버튼 - 하강(피치조정-스로틀Throttle레버 모두 저속)
        btnDown = (Button) findViewById(R.id.btnDown);
        btnDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "G", "g");
                return false;
            }
        });

        // 버튼 - 좌회전(요우Yaw조정-러더레버 좌상우하고속)
        btnRotateLeft = (Button) findViewById(R.id.btnRotateLeft);
        btnRotateLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "F", "f");
                return false;
            }
        });

        // 버튼 - 우회전(요우Yaw조정-러더레버 상좌우하고속)
        btnRotateRight = (Button) findViewById(R.id.btnRotateRight);
        btnRotateRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                handleToggleButton(event, "H", "h");
                return false;
            }
        });
    }

    private void handleToggleButton(MotionEvent event, String signalDown, String signalUp) {
        // 버튼 동작 감지
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            // 다운 신호 보내기
            sendBluetoothSignal(signalDown);

            // 진동
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(60);
        } else if(event.getAction() == MotionEvent.ACTION_UP) {
            // 신호를 보내다.
            sendBluetoothSignal(signalUp);

            // 진동
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(30);
        }
    }

    private void sendBluetoothSignal(String signal) {
        if(btSocket != null)
        {
            try
            {
                btSocket.getOutputStream().write(signal.toString().getBytes());
            } catch (IOException e) {
                //Toast.makeText(getApplicationContext(), "신호를 보낼 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 연결 헤제 메뉴 인플레이팅 출력
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu_disconnect, menu);

        // 기본값 반환
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 선택한 변수
        int selectID = item.getItemId();

        // 연결 해제
        if(selectID == R.id.action_disconnect) {
            if(btSocket != null) {
                try {
                    btSocket.close();
                } catch(IOException e) {
                    Toast.makeText(getApplicationContext(), "연결을 해제할 수 없습니다...", Toast.LENGTH_LONG).show();
                }
            }

            finish();
        }

        // 종료하기
        if(selectID == R.id.action_kill) {
            // 대화상자 열기
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder
                .setCancelable(false)
                .setTitle("드론 끄기")
                .setMessage("드론 조종을 종료 하시겠습니까?")
                .setPositiveButton("예, 종료합니다.", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sendBluetoothSignal("Q");
                    }
                })
                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        // 기본값 반환
        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  //멀티 스레드에서 UI 핸들링
    {
        private boolean ConnectSuccess = true; //기본 연결 설정 값 True

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(Controller.this, "연결 중...", "잠시만 기다려 주세요!!!");  //진행 대화 상자 표시
        }

        @SuppressLint("MissingPermission")
        @Override
        protected Void doInBackground(Void... devices) //진행 대화 상자가 표시되는 동안 연결은 백그라운드에서 수행됩니다.
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//페어링된 모바일 블루투스 장치 가져오기
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//장치의 주소에 연결하고 사용 가능한지 확인합니다.
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//RFCOMM(SPP) 연결 객체 생성
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//연결 시작
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//시도가 실패하면 여기에서 예외를 확인할 수 있습니다.
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //doInBackground 후에 모든 것이 잘 되었는지 확인합니다.
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Toast.makeText(getApplicationContext(), "연결 실패. 다시 시도해 주세요!", Toast.LENGTH_LONG).show();
                finish();
            }
            else
            {
                Toast.makeText(getApplicationContext(), "연결됨!", Toast.LENGTH_LONG).show();
                isBtConnected = true;
            }

            progress.dismiss();
        }
    }
}
