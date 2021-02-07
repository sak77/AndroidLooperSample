package com.example.sshriwas.androidloopersample;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


/***
 * To communicate between background thread and main thread in Android there are few options -
 * - Activity.runOnUiThread()
 * - View.post()
 * - View.postDelayed()
 * - Looper, MessageQueue, Messages/Runnables and Handler
 * Purpose of this app is to demonstrate communication between threads using these concepts.
 */
public class MainActivity extends AppCompatActivity {
    TextView txtOutput;
    //Use Ctrl + J for templates
    private static final String TAG = "MainActivity";
    int optionSelected = -1;
    RadioGroup radioGroup;
    int sleepForMillis = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtOutput = findViewById(R.id.txtOutput);
        Button btnSleep = findViewById(R.id.btnSleep);
        btnSleep.setOnClickListener(view -> {
            txtOutput.setText("Sleeping...");
            //Thread.run() simply invokes code in run method on current thread.
            //Thread.start() creates a separate thread.
            //So use start() to execute on background thread.
            switch (optionSelected) {
                case R.id.rbRunOnUiThread:
                    new ThreadUsingRunOnUiThread().start();
                    break;
                case R.id.rbViewPost:
                    new ThreadUsingViewPost().start();
                    break;
                case R.id.rbViewPostDelayed:
                    new ThreadUsingViewPostDelayed().start();
                    break;
                case R.id.rbHandlerSendMessage:
                    new ThreadUsingHandler().start();
                    break;
                case R.id.rbHandlerPostRunnable:
                    new ThreadUsingHandlerPostRunnable().start();
                    break;
                default:
                    Log.d(TAG, "onCreate: Undefined id");
            }
        });

        radioGroup = findViewById(R.id.rgOption);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            btnSleep.setEnabled(true);
            group.setEnabled(false);
            optionSelected = checkedId;
        });
    }

    //Note - Just Implementing Runnable does not automatically cause its run() to be executed on
    // a separate thread. This is because Runnable is simply a functional interface with a single
    // method run(). For background tasks, extend the Thread class.
    private class TestRunnable implements Runnable {
        @Override
        public void run() {
        }
    }

    private class ThreadUsingRunOnUiThread extends Thread {

        @Override
        public void run() {
            super.run();
            //SystemClock.sleep is similar to Thread.sleep() plus, you don't need the
            //try catch block....
            SystemClock.sleep(sleepForMillis);
            MainActivity.this.runOnUiThread(MainActivity.this::wakeUp);
            Log.d(TAG, "onCreate: R.id.rbRunOnUiThread");
        }
    }

    private class ThreadUsingViewPost extends Thread {
        @Override
        public void run() {
            super.run();
            SystemClock.sleep(sleepForMillis);
            txtOutput.post(MainActivity.this::wakeUp);
            Log.d(TAG, "onCreate: R.id.rbViewPost");
        }
    }

    private class ThreadUsingViewPostDelayed extends Thread {
        @Override
        public void run() {
            super.run();
            txtOutput.postDelayed(MainActivity.this::wakeUp, sleepForMillis);
            Log.d(TAG, "onCreate: R.id.rbViewPostDelayed");
        }
    }

    private void wakeUp() {
        txtOutput.setText("Woke up!");
        radioGroup.setEnabled(true);
    }

    private class ThreadUsingHandler extends Thread {

        @Override
        public void run() {
            super.run();
            /*
            The main components here are -
            1. MessageQueue - a queue that holds list of messages/runnables which are handled by the Handler.
            2. Looper - An infinite for loop that loops through messages in the MessageQueue. And
            dispatches them to the Handler. There can be only one message queue and looper for each
            thread in the application.
            3. Handler - Is associated with a thread and its looper. It sends messages to the message queue
            and also handles messages which are dispatched from the message queue. There can be multiple
            handlers for a given Looper and message queue.
            4. Message - Contains info/data which tells handler what task it needs to perform.
             */

            /*
            Background threads do not create a message queue or Looper
            by default. So before instantiating the Handler instance it is necessary to
            create a message queue and looper for the current thread. This is accomplished using
            Looper.prepare(). Also, after Handler instance is created, call Looper.loop()
            to begin looping through the message queue.

            Get handler for Main Thread's Looper using Looper.getMainLooper().
            Get handler for current thread's looper using Looper.myLooper().

            One more point to note here is that if the handler does not have reference to the main
            thread's looper then it CANNOT update the UI.
             */
            Log.d(TAG, "onCreate: R.id.rbHandlerSendMessage");

                /*
                Anonymous inner class by default holds implicit reference to its parent class, which is
                MainActivity in this case. That is why we are able to reference txtOutput inside it.

                However, if the background task is long running, then it can pose a risk of memory leak.
                What happens if the user rotates the device when the background task is in progress?

                Ideally the system will delete the current activity instance and create a new instance.
                However, this will not be possible since the background thread is still running and it
                holds reference of the parent class. So it will leak the activity class. This has to
                be avoided as far as possible.

                Either make the handler impl static, but then it cannot access member variables of the
                parent class. Or use a weak reference....
                 */

                /*
                Update 04 Feb 2021 - Default Handler constructor new Handler() is deprecated.
                 */
            Handler myHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    wakeUp();
                }

                //Not sure whats difference between handle message and dispatch
                //but its recommended to use handlemessage()
                @Override
                public void dispatchMessage(Message msg) {
                    super.dispatchMessage(msg);
                }
            };
            SystemClock.sleep(sleepForMillis);

            /*
            Instead of using the public constructor to define a message, better use
            the handler.obtainMessage() to create message instance.

            This is more efficient since it will try and recycle existing message instances
            instead of creating a new instance every time.
             */
            //Message message = new Message();
            Message message = myHandler.obtainMessage();
            message.obj = "Done!";
            //myHandler.sendMessage(message);
            //We can also send empty message just to indicate that the task is completed.
            myHandler.sendEmptyMessage(0);
        }
    }

    private class ThreadUsingHandlerPostRunnable extends Thread {
        @Override
        public void run() {
            super.run();
            //Since our handler is linked to MessageQueue of main thread. So we dont need to use
            //Looper.prepare();
            Log.d(TAG, "onCreate: R.id.rbHandlerPostRunnable");
            SystemClock.sleep(sleepForMillis);
            Handler mainThreadHandler = new Handler(Looper.getMainLooper());
            WakeUpRunnable myRunnable = new WakeUpRunnable();
            mainThreadHandler.post(myRunnable);
        }
    }

    private class WakeUpRunnable implements Runnable {

        @Override
        public void run() {
            wakeUp();
        }
    }


    /*
    Here i demonstrate how to create and link Handler to background thread...
     */
    private class ThreadUsingHandlerForBackgroundThreads extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                //Since background threads do not have a MessageQueue and Looper by default,
                //first use Looper.prepare() to setup the same
                Looper.prepare();
                //Instead of Looper.getMainLooper(), use Looper.myLooper() to link handler to
                //MessageQUeue of current thread...
                Handler myBackgroundHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        //This is handled on the message queue of the background thread...
                        //below UI update will not work.
                        //txtOutput.setBackgroundColor(Color.RED);

                        //For some reason setText() method works from non-ui thread. But other UI updates require
                        //handler that is created and linked to main thread's MessageQueue and Looper.
                        //txtOutput.setText("Hello from background thread.");

                        Log.d(TAG, "handleMessage: Got message " + msg.what);
                    }
                };

                myBackgroundHandler.sendEmptyMessage(39);
                //Start Looper to loop through MessageQueue
                //The call to loop() doesn’t return;
                // from the moment you call this method, your thread is processing messages until
                // the quit() method is called on its Looper.
                Looper.loop();
            }finally {
                //Quit local looper safely
                Looper.myLooper().quitSafely();
            }
        }
    }

    /**
     * After this experiment my understanding about Loopers and Handlers is as follows-
     * The main benefit of the Looper and Handler class is to enable communication between
     * the worker threads and the main UI thread. Worker threads can update the UI thread
     * by creating instance of Handler and passing reference of the main thread's looper
     * via Looper.getMainLooper().
     */
}