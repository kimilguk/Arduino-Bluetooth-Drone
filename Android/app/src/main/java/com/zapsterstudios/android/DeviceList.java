package com.zapsterstudios.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;


public class DeviceList extends ActionBarActivity
{
    //UI용 전역 변수지정
    Button btnPaired;
    ListView devicelist;

    //블루투스용 전용 변수지정
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        //UI 객체 생성
        btnPaired = (Button)findViewById(R.id.button);
        devicelist = (ListView)findViewById(R.id.listView);

        //블루투스 객체 생성
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(myBluetooth == null)
        {
            //기기에 블루투스 어댑터가 없다는 메시지 표시
            Toast.makeText(getApplicationContext(), "블루투스 장치를 사용할 수 없음", Toast.LENGTH_LONG).show();

            //액티비티 종료
            finish();
        }
        else if(!myBluetooth.isEnabled())
        {
            //사용자에게 블루투스를 켜도록 요청
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        }

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                pairedDevicesList();
            }
        });

    }

    @SuppressLint("MissingPermission")
    private void pairedDevicesList()
    {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress()); //블루투스 장치의 이름과 주소를 가져옵니다.
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "스마트폰에 페어링된 Bluetooth 장치를 찾을 수 없음.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //목록에서 장치를 클릭할 때 호출되는 메서드

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            // UI에서 장치 MAC 주소 마지막 17자 가져오기
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // 드론 조정 액티비티를 인텐트 객체에 담는다.
            Intent i = new Intent(DeviceList.this, Controller.class);

            //액티비티에 데이터 전달
            i.putExtra(EXTRA_ADDRESS, address); //드론 조정 액티비티로 전송될 내용이다.
            startActivity(i);
        }
    };
}
