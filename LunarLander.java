/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.lunarlander;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
//import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.example.android.lunarlander.LunarView.LunarThread;

/**
 * This is a simple LunarLander activity that houses a single LunarView. It
 * demonstrates...
 * <ul>
 * <li>animating by calling invalidate() from draw()
 * <li>loading and drawing resources
 * <li>handling onPause() in an animation
 * </ul>
 */
public class LunarLander extends Activity implements OnAccountsUpdateListener, OnClickListener {

	private SimulationView mSimulationView;
	private SensorManager mSensorManager;
	private PowerManager mPowerManager;
	private WindowManager mWindowManager;
	private Display mDisplay;
	private WakeLock mWakeLock;
	private ArrayList<AccountData> mAccounts;
	private AccountAdapter mAccountAdapter;

    private static final int MENU_EASY = 1;

    private static final int MENU_HARD = 2;

    private static final int MENU_MEDIUM = 3;

    private static final int MENU_PAUSE = 4;

    private static final int MENU_RESUME = 5;

    private static final int MENU_START = 6;

    private static final int MENU_STOP = 7;

    /** A handle to the thread that's actually running the animation. */
    private LunarThread mLunarThread;

    /** A handle to the View in which the game is running. */
    private LunarView mLunarView;

    // the play start button
    private Button mButton;
    private Button mButtonFire;
    private Button mButtonLeft;
    private Button mButtonRight;

    private Spinner mAccountSpinner;

    /** Is the engine burning? */
    private boolean mEngineFiring;

    /** Currently rotating, -1 left, 0 none, 1 right. */
    private int mRotating;

   /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     *
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);
        menu.add(0, MENU_EASY, 0, R.string.menu_easy);
        menu.add(0, MENU_MEDIUM, 0, R.string.menu_medium);
        menu.add(0, MENU_HARD, 0, R.string.menu_hard);

        return true;
    }

    public void register() {
    	mSimulationView.register();
	}

    public void unregister() {
    	mSimulationView.unregister();
	}

    /**
     * Invoked when the user selects an item from the Menu.
     *
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_START:
                mLunarThread.doStart();
                mButton.setText("Pause");
				mWakeLock.acquire();
				register();
                return true;
            case MENU_STOP:
                mLunarThread.setState(LunarThread.STATE_LOSE,
                getText(R.string.message_stopped));
                mButton.setText("Start");
				if (mWakeLock.isHeld())
					mWakeLock.release();//already released?
				unregister();
                return true;
            case MENU_PAUSE:
                mLunarThread.pause();
                mButton.setText("Unpause");
				mWakeLock.release();
                return true;
            case MENU_RESUME:
                mLunarThread.unpause();
                mButton.setText("Pause");
				mWakeLock.acquire();
                return true;
            case MENU_EASY:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_EASY);
                return true;
            case MENU_MEDIUM:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_MEDIUM);
                return true;
            case MENU_HARD:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_HARD);
                return true;
        }

        return false;
    }

    /**
     * Invoked when the Activity is created.
     *
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.lunar_layout);

        // get handles to the LunarView from XML, and its LunarThread
        mLunarView = (LunarView) findViewById(R.id.lunar);
        mLunarThread = mLunarView.getThread();

        mButton = (Button)findViewById(R.id.Button01);
        mButton.setOnClickListener(this);

        mButtonFire = (Button)findViewById(R.id.Button02);
        mButtonFire.setOnClickListener(this);

        mButtonLeft = (Button)findViewById(R.id.Button03);
        mButtonLeft.setOnClickListener(this);

        mButtonRight = (Button)findViewById(R.id.Button04);
        mButtonRight.setOnClickListener(this);

        mAccountSpinner = (Spinner) findViewById(R.id.accountSpinner);

        mEngineFiring = false;
        mRotating = 0;

        // give the LunarView a handle to the TextView used for messages
        mLunarView.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            mLunarThread.setState(LunarThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            mLunarThread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay();
		mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());
		mSimulationView = new SimulationView(this);
		//setContentView(mSimulationView);

		//Prepare model for account spinner
		mAccounts = new ArrayList<AccountData>();
		mAccountAdapter = new AccountAdapter(this, mAccounts);
		mAccountSpinner.setAdapter(mAccountAdapter);
		AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);

        mAccountSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {
			AccountData data = (AccountData) mAccountSpinner.getSelectedItem();
				mLunarThread.LunarName = data.toString();
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // We don't need to worry about nothing being selected, since Spinners don't allow
                // this.
            }
        });
    }///onResume is next

    @Override
    public void onDestroy() {
        // Remove AccountManager callback
        AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        super.onDestroy();
    }///surfaceDestroyed is next

    @Override
    protected void onResume() {///after onCreate, after onRestart
    	super.onResume();
    	Log.w("resume", "resume");
    }///onAccountsUpdated/surfaceCreated is next

    @Override
    protected void onStop() {///after onSaveInstanceState
    	super.onStop();
    	Log.w("stop", "stop");
    }///onDestroy is next?

    @Override
    protected void onRestart() {///opened app again
    	super.onRestart();
    	Log.w("restart", "restart");
    }///onResume is next

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.w("restore", "restore");
    }

    /**
     * Handles component interaction
     * 
     * @param v The object which has been clicked
     */
    public void onClick(View v) {
    	int mode =	mLunarThread.getMode();
///    	mLunarView.setTextView("" + mSimulationView.accelerating[0] + "\n" + mSimulationView.accelerating[1]);
    	if (mButton.equals(v)) {
    		synchronized (mLunarThread.getSurfaceHolder()) {
        		if ((mode == 1) || (mode == 3) || (mode == 5)) {
        			mButton.setText("Pause");
        			mLunarThread.doStart();
					mWakeLock.acquire();
					register();
        		} else if (mode == 2/*STATE_PAUSE*/) {
        			mButton.setText("Pause");
            		mLunarThread.unpause();
					mWakeLock.acquire();
        		} else if (mode == 4/*STATE_RUNNING*/) {
        			mButton.setText("Unpause");
            		onPause();
					mWakeLock.release();//already released?
        		}
    		}
    	}
        else if (mButtonFire.equals(v)) {
            synchronized (mLunarThread.getSurfaceHolder()) {
            	if (mode == 4) {
            		mEngineFiring = !mEngineFiring;
            		mLunarThread.setFiring(mEngineFiring);
            	}
            }
		}
        else if (mButtonLeft.equals(v)) {
            synchronized (mLunarThread.getSurfaceHolder()) {
            	if (mode == 4) {
            		if (mRotating == 0)
            			mRotating = -1;
            		else
            			mRotating = 0;
            		mLunarThread.setRotating(mRotating);
            	}
            }
        }
        else if (mButtonRight.equals(v)) {
            synchronized (mLunarThread.getSurfaceHolder()) {
            	if (mode == 4) {
            		if (mRotating == 0)
            			mRotating = 1;
            		else
            			mRotating = 0;
            		mLunarThread.setRotating(mRotating);
            	}
            }
        }
    }
    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        mLunarView.getThread().pause(); // pause game when Activity pauses
        super.onPause();
    }///onSaveInstanceState/onStop is next

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mLunarThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }///onStop is next

    public void onAccountsUpdated(Account[] a) {
    	Log.w(this.getClass().getName(), "Account list update detected");
    	try {
			// Clear out any old data to prevent duplicates
            mAccounts.clear();
            // Get account data from system
            AuthenticatorDescription[] accountTypes = AccountManager.get(this).getAuthenticatorTypes();

            for (int i = 0; i < a.length/**1**/; i++) {
				String systemAccountType = a[i].type;///"com.android.email";
                AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType,
                    accountTypes);
                AccountData data = new AccountData(a[i].name/**"abc"**/, ad);
                mAccounts.add(data);
/*
                systemAccountType = "com.android.exchange";
                ad = getAuthenticatorDescription(systemAccountType,
					accountTypes);
                data = new AccountData("xyz", ad);
                mAccounts.add(data);
*/    		}
    	} catch (Exception e) {//IllegalArgumentException
   		 Log.w(this.getClass().getName(), "catch");
    	}
		try {
			// Update the account spinner
            mAccountAdapter.notifyDataSetChanged();
		} catch (Exception e) {//IllegalArgumentException
			Log.w(this.getClass().getName(), "catch");
		}
	}///surfaceCreated is next

    private static AuthenticatorDescription getAuthenticatorDescription(String type,
            AuthenticatorDescription[] dictionary) {
        for (int i = 0; i < dictionary.length; i++) {
            if (dictionary[i].type.equals(type)) {
                return dictionary[i];
            }
        }
        // No match found
        throw new RuntimeException("Unable to find matching authenticator");
    }

    class SimulationView extends View implements SensorEventListener {
		private Sensor mAccelerometer;
		public float accelerating[] = new float[2];

		public SimulationView(Context context) {
			super(context);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;

            switch (mDisplay.getRotation()) {
            		case Surface.ROTATION_0:
            			if      (event.values[0] < -2)//0)
            				mRotating = -1;
            			else if (event.values[0] >  2)//0)
            				mRotating =  1;
            			else
            				mRotating =  0;
            			if      (event.values[1] > 0)
            				mEngineFiring = true;
            			else
            				mEngineFiring = false;
            			break;
            		case Surface.ROTATION_90:
            			if      (event.values[0] > 0)
            				mEngineFiring = true;
            			else
            				mEngineFiring = false;
            			if      (event.values[1] < 0)
            				mRotating = -1;
            			else if (event.values[1] > 0)
            				mRotating =  1;
            			else
            				mRotating =  0;
                		break;
            		case Surface.ROTATION_180:
            			if      (event.values[0] < -2)//0)
            				mRotating = -1;
            			else if (event.values[0] >  2)//0)
            				mRotating =  1;
            			else
            				mRotating =  0;
            			if      (event.values[1] > 0)
            				mEngineFiring = true;
            			else
            				mEngineFiring = false;
            			break;
            		case Surface.ROTATION_270:
            			if      (event.values[0] > 0)
            				mEngineFiring = false;
            			else
            				mEngineFiring = true;
            			if      (event.values[1] < 0)
            				mRotating = -1;
            			else if (event.values[1] > 0)
            				mRotating =  1;
            			else
            				mRotating =  0;
            			break;
				}
				accelerating[0] =	event.values[0];
				accelerating[1] =	event.values[1];
				mLunarThread.setRotating(mRotating);
				mLunarThread.setFiring(mEngineFiring);
            }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void register() {
			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        public void unregister() {
			mSensorManager.unregisterListener(this);
        }
    }

    private class AccountData {
        private String mName;
        private String mType;
        private CharSequence mTypeLabel;
        private Drawable mIcon;

		public AccountData(String name, AuthenticatorDescription description) {
            mName = name;
            if (description != null) {
                mType = description.type;

                // The type string is stored in a resource, so we need to convert it into something
                // human readable.
                String packageName = description.packageName;
                PackageManager pm = getPackageManager();

                if (description.labelId != 0) {
                    mTypeLabel = pm.getText(packageName, description.labelId, null);
                    if (mTypeLabel == null) {
                        throw new IllegalArgumentException("LabelID provided, but label not found");
                    }
                } else {
                    mTypeLabel = "";
                }

                if (description.iconId != 0) {
                    mIcon = pm.getDrawable(packageName, description.iconId, null);
                    if (mIcon == null) {
                        throw new IllegalArgumentException("IconID provided, but drawable not " +
                                "found");
                    }
                } else {
                    mIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                }
            }
		}

        public String getName() {
            return mName;
        }

        public String getType() {
            return mType;
        }

        public CharSequence getTypeLabel() {
            return mTypeLabel;
        }

        public Drawable getIcon() {
            return mIcon;
        }

        public String toString() {
			return mName + " " + mTypeLabel + " " + mType;
        }
    }

    private class AccountAdapter extends ArrayAdapter<AccountData> {
    	public AccountAdapter(Context context, ArrayList<AccountData> accountData) {
			super(context, android.R.layout.simple_spinner_item, accountData);
			setDropDownViewResource(R.layout.account_entry);
		}

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // Inflate a view template
            if (convertView == null) {
                LayoutInflater layoutInflater = getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.account_entry, parent, false);
            }
            TextView firstAccountLine = (TextView) convertView.findViewById(R.id.firstAccountLine);
            TextView secondAccountLine = (TextView) convertView.findViewById(R.id.secondAccountLine);
            TextView thirdAccountLine = (TextView) convertView.findViewById(R.id.thirdAccountLine);
            ImageView accountIcon = (ImageView) convertView.findViewById(R.id.accountIcon);

            // Populate template
            AccountData data = getItem(position);
            firstAccountLine.setText(data.getName());
            secondAccountLine.setText(data.getTypeLabel());
            thirdAccountLine.setText(data.getType());
            Drawable icon = data.getIcon();
            if (icon == null) {
                icon = getResources().getDrawable(android.R.drawable.ic_menu_search);
            }
            accountIcon.setImageDrawable(icon);
            return convertView;
        }
    }
}
