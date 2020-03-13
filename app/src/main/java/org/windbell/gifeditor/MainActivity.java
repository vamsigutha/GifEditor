package org.windbell.gifeditor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.net.Uri;

import java.io.FileNotFoundException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    String[] mPermissionList = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    public static final int REQUEST_OPEN_GIF = 1;
    public static final int REQUEST_SAVE_GIF = 2;
    public static final int REQUEST_PICK_IMAGE = 11101;
    public GifImageView gifimageview;
    public GifDrawable gifDrawable;
    private SeekBar seekBar1, seekBar2;
    public Uri gifuri;
    private int editbegin, editend;
    private Button button1, button2, button3, button4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gifimageview = (GifImageView) findViewById(R.id.gifimageview);
        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);


        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, mPermissionList, REQUEST_OPEN_GIF);
                } else {
                    getImage();
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, mPermissionList, REQUEST_SAVE_GIF);
                } else {
                    saveimage();
                }
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gifplay();
            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gifpause();
            }
        });

        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (gifDrawable != null) {
                    gifDrawable.seekTo(progress * gifDrawable.getDuration() / gifDrawable.getNumberOfFrames());
                    gifpause();
                    editbegin = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (gifDrawable != null) {
                    gifDrawable.seekTo((gifDrawable.getNumberOfFrames() - progress) * gifDrawable.getDuration() / gifDrawable.getNumberOfFrames());
                    gifpause();
                    editend = gifDrawable.getNumberOfFrames() - progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void gifplay() {
        if (gifDrawable != null) {
            gifDrawable.start();
        }
    }

    public void gifpause() {
        if (gifDrawable != null) {
            gifDrawable.pause();
        }
    }

    private void getImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/gif"),
                    REQUEST_PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/gif");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        }
    }

    private void saveimage() {
        ContentResolver cr = getContentResolver();
        try {
            InputStream is = getContentResolver().openInputStream(gifuri);
            GifDecoder gifDecoder = new GifDecoder();
            int code = gifDecoder.read(is);

            if (code == GifDecoder.STATUS_OK) {
                GifDecoder.GifFrame[] frameList = gifDecoder.getFrames();

                File pathFile = new File(getExternalCacheDir().toString() + "/GifEditor");
                File gifFile = new File(pathFile, System.currentTimeMillis() + ".gif");
                if (!pathFile.exists())
                    pathFile.mkdirs();
                if (!gifFile.exists())
                    try {
                        gifFile.createNewFile();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                OutputStream os;
                try {
                    os = new FileOutputStream(gifFile);
                    AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
                    gifEncoder.start(os);
                    int i;
                    for (i = editbegin; i < editend; i++) {
                        gifEncoder.addFrame(frameList[i].image);
                    }
                    gifEncoder.setDelay(frameList[0].delay);
                    gifEncoder.setRepeat(0);
                    gifEncoder.finish();
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri uri = Uri.fromFile(gifFile);
                    intent.setData(uri);
                    sendBroadcast(intent);

                    Toast.makeText(MainActivity.this, "图片保存在：" + gifFile, Toast.LENGTH_SHORT).show();

                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } else if (code == gifDecoder.STATUS_FORMAT_ERROR) {//图片格式不是GIF
                Toast.makeText(MainActivity.this, "图片格式不是GIF",Toast.LENGTH_SHORT).show();
            } else {//图片读取失败
                Toast.makeText(MainActivity.this, "图片读取失败",Toast.LENGTH_SHORT).show();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean writeExternalStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        boolean readExternalStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
        switch (requestCode) {
            case REQUEST_OPEN_GIF:
                if (grantResults.length > 0 && writeExternalStorage && readExternalStorage) {
                    getImage();
                } else {
                    Toast.makeText(this, "请设置必要权限", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_SAVE_GIF:
                if (grantResults.length > 0 && writeExternalStorage && readExternalStorage) {
                    saveimage();
                } else {
                    Toast.makeText(this, "请设置必要权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:
                    if (data != null) {

                        gifimageview.setImageURI(data.getData());
                        gifDrawable = (GifDrawable) gifimageview.getDrawable();
                        gifuri = data.getData();


                        if (gifDrawable != null) {
                            seekBar1.setMax(gifDrawable.getNumberOfFrames());
                            seekBar2.setMax(gifDrawable.getNumberOfFrames());
                        }
                    } else {
                        Toast.makeText(this, "图片损坏，请重新选择", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

}
