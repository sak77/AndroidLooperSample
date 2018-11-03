package com.example.sshriwas.androidloopersample;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;


/***
 * The purpose of this app is to explore some core Android Concepts.
 * Namely: Looper, MessageQueue, Messages, Handler & Messengers
 * We also want to see if we can communicate between threads using these concepts.
 * For this app we will start a thread. Add a MessageQueue via Looper, define
 * a handler to process the messages and later a messenger to maybe send
 * messages from different threads.
 */
public class MainActivity extends AppCompatActivity {
    TextView txtOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextInputEditText editText = findViewById(R.id.txtInput);
        txtOutput = findViewById(R.id.txtOutput);
        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(view -> {
            //Take input value and pass it to thread
            String input = editText.getText().toString();
            MyCalculateThread myCalculateThread = new MyCalculateThread(Integer.parseInt(input));
            myCalculateThread.start();
        });
    }


    private class MyCalculateThread extends Thread {
        private int mSeed;

        MyCalculateThread(int seed) {
            mSeed = seed;
        }

        @Override
        public void run() {
            super.run();
            //Try updating UI from here. Get handler for Main Thread's Looper using getMainLooper()
            //Handler myHandler = new Handler(Looper.getMainLooper()) {

            try{
            /*
            Default Handler constructor - creates a Handler and associates it with the Looper of the
            current thread. So if you are using the default constructor, be sure to first create a
            Looper for the thread by using Looper.prepare(). Ofcourse you need to also call
            Looper.loop() so that the Looper can start dispatching messages once the handler sends them.
            One more point to note here is that if the handler does not have reference to the main thread's looper
            then it CANNOT update the UI. Below setText() only works due to some wierd Andriod behavior. Otherwise
            trying to set bkg color will throw an exception.
             */
                Looper.prepare();
                //Define handler, which will send messages and also handle them...
                Handler myHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        try {
                            sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //txtOutput.setText(msg.obj.toString());
/*
                        txtOutput.setBackgroundColor(
                                Color.rgb(200, 250, Integer.parseInt(msg.obj.toString())));
*/
                    }
                    //Not sure whats difference between handle message and dispatch
                    //but its recommended to use handlemessage()
                    @Override
                    public void dispatchMessage(Message msg) {
                        super.dispatchMessage(msg);
                    }
                };

                //Prepare the messagequeue for the looper, by sending messages to it
                for (int i = 0; i < 10; i++) {
                    int value = myCalculateFunction(mSeed);
                    Message message = new Message();
                    message.obj = value;
                    myHandler.sendMessage(message);
                }
                //Call Looper.loop to loop through the messages in the messagequeue
                //The call to loop() doesn’t return;
                // from the moment you call this method, your thread is processing messages until the quit() method is called on its Looper.
                Looper.loop();

            }finally {
                Looper.myLooper().quitSafely();
            }
        }
    }

    private int myCalculateFunction(int seed) {
        return (int) (seed * SystemClock.currentThreadTimeMillis());
    }

    /**
     * After this experiment my understanding about Loopers and Handlers is as follows-
     * The main benefit of the Looper and Handler class is to enable communication between
     * the worker threads and the main UI thread. So after worker threads has completed their
     * tasks they can update the UI thread by creating instance of Handler and passing reference
     * of the main thread's looper via Looper.getMainLooper().
     *
     * You can also create a default Handler where you don't pass any reference to the Handler
     * or just reference to current thread's Looper. But i didn't find this to be of too much
     * use in practical situations.
     */
}