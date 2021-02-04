package com.example.sshriwas.androidloopersample;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
    private static final String TAG = "MainActivity";
    private int mCount;

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
            ThreadUsingHandler myCalculateThread = new ThreadUsingHandler(Integer.parseInt(input));
            //Thread.run() simply invokes code in run method on the thread.
            //However Thread.start() creates a separate thread. So use start() to execute on
            // background thread.
            myCalculateThread.start();

            //new MyCalculateRunnable().run();

            //CalculateThread2 calculateThread2 = new CalculateThread2(Integer.parseInt(input));
            //calculateThread2.start();
        });
    }

    //Note - Just Implementing Runnable does not automatically cause its run() to be executed on
    // a separate thread. This is because Runnable is simply a functional interface with a single
    // method run(). For background tasks, extend the Thread class only.
    private class MyCalculateRunnable implements Runnable {

        @Override
        public void run() {

            for (int i = 0; i < 10; i++) {
                SystemClock.sleep(2000);
                Log.i(TAG, "" + i);
            }

        }
    }

    /*
    Instead of Handlers, this thread uses Activity.runOnUiThread() to communicate between worker
    thread and the main thread.
     */
    private class ThreadUsingRunOnUiThread extends Thread {

        int mSeed;

        ThreadUsingRunOnUiThread(int seed) {
            mSeed = seed;
        }

        @Override
        public void run() {
            super.run();

            for (int i = 0; i < 10; i++) {
                long value = myCalculateFunction(i);
                Log.v(TAG, "Output - " + value);
                MainActivity.this.runOnUiThread(() -> {
                    txtOutput.setText("" + value);
                    txtOutput.setBackgroundColor(
                            Color.rgb(200, 250, value));
                });
                //SystemClock.sleep is similar to Thread.sleep() plus, you dont need the
                //try catch block....
                SystemClock.sleep(1000);
            }
        }
    }


    /*
    This thread uses Handler to communicate back to the UI thread.
     */
    private class ThreadUsingHandler extends Thread {
        private int mSeed;

        ThreadUsingHandler(int seed) {
            mSeed = seed;
        }

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
            Handler's default constructor takes no argument and it associates the handler with the
            Looper of the current thread. But background threads do not create a message queue or Looper
            by default. So before instantiating the default Handler instance, it is necessary to
            create a message queue and looper for the current thread. This is accomplished using
            Looper.prepare(). So call this before you create the Handler instance. Also, after
            Handler instance is created, call Looper.loop() to begin looping through the message queue.

            Get handler for Main Thread's Looper using Looper.getMainLooper().

            One more point to note here is that if the handler does not have reference to the main
            thread's looper then it CANNOT update the UI.
             */

            /* Handler for main thread.....
            Handler myHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }
            };*/

            //Handler for current thread
            try {
                Looper.prepare();
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
                Implicitly choosing a Looper during Handler construction can lead to bugs where operations
                are silently lost (if the Handler is not expecting new tasks and quits), crashes
                (if a handler is sometimes created on a thread without a Looper active), or race conditions,
                where the thread a handler is associated with is not what the author anticipated.
                Instead, use an Executor or specify the Looper explicitly, using Looper#getMainLooper, {link android.view.View#getHandler}, or similar.
                If the implicit thread local behavior is required for compatibility,
                 use new Handler(Looper.myLooper()) to make it clear to readers.
                 */
                Handler myHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        //Same as Thread.sleep() but without the try catch...
                        SystemClock.sleep(3000);
                        Log.v(TAG, "Handling message obj - " + msg.obj.toString());

                    /*
                    This will work only if handler is associated with the main thread...

                        txtOutput.setText(msg.obj.toString());
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

                for (int i = 0; i < 10; i++) {
                    long value = myCalculateFunction(mSeed);

                    /*
                    Instead of using the public constructor to define a message, better use
                    the handler.obtainMessage() to create message instance.

                    This is more efficient since it will try and recycle existing message instances
                    instead of creating a new instance every time.
                     */
                    //Message message = new Message();
                    Message message = myHandler.obtainMessage();
                    message.obj = "Message value " + value;
                    Log.v(TAG, "Sending message obj - " + message.obj.toString());
                    myHandler.sendMessage(message);
                }

                //The call to loop() doesn’t return;
                // from the moment you call this method, your thread is processing messages until
                // the quit() method is called on its Looper.
                Looper.loop();

            } finally {
                Looper.myLooper().quitSafely();
            }
        }
    }

    private long myCalculateFunction(int seed) {
        mCount+=4;
        return (seed * mCount);
    }

    /**
     * After this experiment my understanding about Loopers and Handlers is as follows-
     * The main benefit of the Looper and Handler class is to enable communication between
     * the worker threads and the main UI thread. So after worker threads has completed their
     * tasks they can update the UI thread by creating instance of Handler and passing reference
     * of the main thread's looper via Looper.getMainLooper().
     */
}