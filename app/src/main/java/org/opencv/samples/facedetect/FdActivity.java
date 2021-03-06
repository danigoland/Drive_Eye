package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.Video;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class FdActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "OCVSample::Activity";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static final int JAVA_DETECTOR = 0;
	private static final int TM_SQDIFF = 0;
	private static final int TM_SQDIFF_NORMED = 1;
	private static final int TM_CCOEFF = 2;
	private static final int TM_CCOEFF_NORMED = 3;
	private static final int TM_CCORR = 4;
	private static final int TM_CCORR_NORMED = 5;

	static private boolean HaarLE = false;
	static private boolean HaarRE = false;
	static private boolean HaarEyeOpen_R = false;
	static private boolean HaarEyeOpen_L = false;

	private int[] accumulationArr = new int[2];

	static Mat openEye;
	static Mat closeEye;

	private int learn_frames = 0;
	private Mat teplateR;
	private Mat teplateL;
	int method = 0;

	static int TotalFrames = 0;
	static int FrameFace = 0;
	static int FrameEyesOpen = 0;
	static int FrameEyesClosed = 0;
	static boolean flag = false;

	private MenuItem mItemFace50;
	private MenuItem mItemFace40;
	private MenuItem mItemFace30;
	private MenuItem mItemFace20;
	private MenuItem mItemType;

	private Mat mRgba;
	private Mat mGray;
	private Mat mHist;
	// matrix for zooming
	private Mat mZoomWindow;
	private Mat mZoomWindow2;

	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private CascadeClassifier mJavaDetectorEye;

	static private CascadeClassifier mJavaDetectorEyeRight;
	static private CascadeClassifier mJavaDetectorEyeLeft;
	static private CascadeClassifier mJavaDetectorEyeOpen;
	static private Mat templateR;
	static private Mat templateL;
	static private Mat templateR_open;
	static private Mat templateL_open;

	private int mDetectorType = JAVA_DETECTOR;
	private String[] mDetectorName;

	private float mRelativeFaceSize = 0.2f;
	private int mAbsoluteFaceSize = 0;

	private int numOfFrames = 0;
	private CameraBridgeViewBase mOpenCvCameraView;

	private SeekBar mMethodSeekbar;
	private TextView mValue;

	double xCenter = -1;
	double yCenter = -1;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV loaded successfully");


					try {
						// load cascade file from application resources
						InputStream is = getResources().openRawResource(
								R.raw.lbpcascade_frontalface);
						File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
						mCascadeFile = new File(cascadeDir,
								"lbpcascade_frontalface.xml");
						FileOutputStream os = new FileOutputStream(mCascadeFile);

						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = is.read(buffer)) != -1) {
							os.write(buffer, 0, bytesRead);
						}
						is.close();
						os.close();

						// --------------------------------- load left eye
						// classificator -----------------------------------
						InputStream iser = getResources().openRawResource(
								R.raw.haarcascade_eye);
						File cascadeDirER = getDir("cascadeER",
								Context.MODE_PRIVATE);
						File cascadeFileER = new File(cascadeDirER,
								"haarcascade_eye_right.xml");
						FileOutputStream oser = new FileOutputStream(cascadeFileER);

						byte[] bufferER = new byte[4096];
						int bytesReadER;
						while ((bytesReadER = iser.read(bufferER)) != -1) {
							oser.write(bufferER, 0, bytesReadER);
						}
						iser.close();
						oser.close();

						mJavaDetector = new CascadeClassifier(
								mCascadeFile.getAbsolutePath());
						if (mJavaDetector.empty()) {
							Log.e(TAG, "Failed to load cascade classifier");
							mJavaDetector = null;
						} else
							Log.i(TAG, "Loaded cascade classifier from "
									+ mCascadeFile.getAbsolutePath());

						mJavaDetectorEyeLeft = new CascadeClassifier(
								cascadeFileER.getAbsolutePath());
						if (mJavaDetectorEyeLeft.empty()) {
							Log.e(TAG, "Failed to load cascade classifier");
							mJavaDetectorEyeLeft = null;
						} else
							Log.i(TAG, "Loaded cascade classifier from "
									+ mCascadeFile.getAbsolutePath());
						mJavaDetectorEyeRight = new CascadeClassifier(
								cascadeFileER.getAbsolutePath());
						if (mJavaDetectorEyeRight.empty()) {
							Log.e(TAG, "Failed to load cascade classifier");
							mJavaDetectorEyeRight = null;
						} else
							Log.i(TAG, "Loaded cascade classifier from "
									+ mCascadeFile.getAbsolutePath());
						mJavaDetectorEyeOpen = new CascadeClassifier(
								cascadeFileER.getAbsolutePath());
						if (mJavaDetectorEyeOpen.empty()) {
							Log.e(TAG, "Failed to load cascade classifier");
							mJavaDetectorEyeOpen = null;
						} else
							Log.i(TAG, "Loaded cascade classifier from "
									+ mCascadeFile.getAbsolutePath());


						cascadeDir.delete();

					} catch (IOException e) {
						e.printStackTrace();
						Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
					}
					mOpenCvCameraView.setCameraIndex(1);
					mOpenCvCameraView.enableFpsMeter();
					mOpenCvCameraView.enableView();

				}
				break;
				default: {
					super.onManagerConnected(status);
				}
				break;
			}
		}
	};

	public FdActivity() {
		mDetectorName = new String[2];
		mDetectorName[JAVA_DETECTOR] = "Java";

		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {


		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.face_detect_surface_view);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);

		mMethodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);
		mValue = (TextView) findViewById(R.id.method);

		mMethodSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				method = progress;
				switch (method) {
					case 0:
						mValue.setText("TM_SQDIFF");
						break;
					case 1:
						mValue.setText("TM_SQDIFF_NORMED");
						break;
					case 2:
						mValue.setText("TM_CCOEFF");
						break;
					case 3:
						mValue.setText("TM_CCOEFF_NORMED");
						break;
					case 4:
						mValue.setText("TM_CCORR");
						break;
					case 5:
						mValue.setText("TM_CCORR_NORMED");
						break;
				}


			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
		mHist = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
		mHist.release();
		mZoomWindow.release();
		mZoomWindow2.release();
	}


	private boolean getccumulation(){
		if (accumulationArr[0] < accumulationArr[1]){
			return true;
		}
		return false;
	}
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		Imgproc.equalizeHist(mGray,mHist);

		if (mAbsoluteFaceSize == 0) {
			int height = mGray.rows();
			if (Math.round(height * mRelativeFaceSize) > 0) {
				mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
			}
		}

		if (mZoomWindow == null || mZoomWindow2 == null)
			CreateAuxiliaryMats();

		MatOfRect faces = new MatOfRect();

		if (mJavaDetector != null)
			mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
					2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
					new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
					new Size());

		Rect[] facesArray = faces.toArray();

		for (int i = 0; i < facesArray.length; i++) {
			Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
					FACE_RECT_COLOR, 3);
			xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
			yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
			Point center = new Point(xCenter, yCenter);

			Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);

			Core.putText(mRgba, "[" + center.x + "," + center.y + "]",
					new Point(center.x + 20, center.y + 20),
					Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
							255));

			Rect r = facesArray[i];
			// compute the eye area
			Rect eyearea = new Rect(r.x + r.width / 8,
					(int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
					(int) (r.height / 3.0));

			// split it
			Rect eyearea_right = new Rect(r.x + r.width / 16,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));

			Rect eyearea_left = new Rect(r.x + r.width / 16
					+ (r.width - 2 * r.width / 16) / 2,
					(int) (r.y + (r.height / 4.5)),
					(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));

			// draw the area - mGray is working grayscale mat, if you want to
			// see area in rgb preview, change mGray to mRgba
			Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
					new Scalar(255, 0, 0, 255), 2);

			Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
					new Scalar(255, 0, 0, 255), 2);


			templateR = get_template(mJavaDetectorEyeRight, eyearea_right, 40);
			templateL = get_template(mJavaDetectorEyeLeft, eyearea_left, 40);

			templateR_open = get_template(mJavaDetectorEyeOpen, eyearea_right, 40);
			templateL_open = get_template(mJavaDetectorEyeOpen, eyearea_left, 40);


			//match_eye
			int cr = (int)match_eye(eyearea_right,templateR,method);
			int cl = (int) match_eye(eyearea_left, templateL, method);
			Log.d("CR", cr+"");

			setAccumulation(cr, true);

			if(numOfFrames <= 4){
				numOfFrames = numOfFrames+1;
			}
			else
			// if we enter here we gave 5 frames
			{
				if(getccumulation() == true){

					//Log.d("closeEye", accumulationArr[1]+"");
					Log.d("close eye", "close eye");

				}else{
					//Log.d("EyeOp", "open eye");
				}
				numOfFrames = 0;
				setAccumulation(0, false);
			}
			Log.d("HaarR","");

			//HaarLE = match_eye(templateL);

			//Log.d("HaarL",""+ );

			Log.d("HaarEyeOpen_R",""+ match_eye(eyearea_right,templateR_open,method));

			//HaarEyeOpen_R = match_eye(templateR_open);
			//HaarEyeOpen_L = match_eye(templateL_open);

			Log.d("HaarEyeOpen_L", ""+ match_eye(eyearea_left,templateL_open,method));

			//Core.putText(mRgba, "Eyes Closed"+(!HaarEyeOpen_R&&!HaarEyeOpen_R), new Point(mRgba.size().width/18, mRgba.size().height/5), Core.FONT_HERSHEY_SCRIPT_COMPLEX, 2, new Scalar(0,255,0),5);





			break;


		}
		return mRgba;
	}


	private void setAccumulation(int cr, boolean flag){
		if(flag == true){

			if (cr < 1){
				accumulationArr[1] = accumulationArr[1] + 1;
			}
			else{
				accumulationArr[0] = accumulationArr[0] + 1;
			}
		}
		else{
			accumulationArr[0] = 0;
			accumulationArr[1] = 0;
		}
		Log.d("arr[0] openEye", "numOfOpens: " + accumulationArr[0]+"  " + "numOfOpens: " +  accumulationArr[1]+"  ");
	}
	private void CreateAuxiliaryMats() {
		if (mGray.empty())
			return;

		int rows = mGray.rows();
		int cols = mGray.cols();

		if (mZoomWindow == null) {
			mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
					+ cols / 10, cols);
			mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
					+ cols / 10, cols);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemFace50 = menu.add("Face size 50%");
		mItemFace40 = menu.add("Face size 40%");
		mItemFace30 = menu.add("Face size 30%");
		mItemFace20 = menu.add("Face size 20%");
		mItemType = menu.add(mDetectorName[mDetectorType]);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item == mItemFace50)
			setMinFaceSize(0.5f);
		else if (item == mItemFace40)
			setMinFaceSize(0.4f);
		else if (item == mItemFace30)
			setMinFaceSize(0.3f);
		else if (item == mItemFace20)
			setMinFaceSize(0.2f);
		else if (item == mItemType) {
			int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
			item.setTitle(mDetectorName[tmpDetectorType]);
		}
		return true;
	}
	private static boolean match_eye(Mat mTemplate) {
		//Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return false;
		}else{
			return true;
		}
	}
	private double match_eye(Rect area, Mat mTemplate, int type) {
		Point matchLoc;
		Mat mROI = mHist.submat(area);
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return 0.0;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		switch (type) {
		case TM_SQDIFF:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
			break;
		case TM_SQDIFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult,
					Imgproc.TM_SQDIFF_NORMED);
			break;
		case TM_CCOEFF:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
			break;
		case TM_CCOEFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult,
					Imgproc.TM_CCOEFF_NORMED);
			break;
		case TM_CCORR:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
			break;
		case TM_CCORR_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult,
					Imgproc.TM_CCORR_NORMED);
			break;
		}

		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
		// there is difference in matching methods - best match is max/min value
		if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
			return mmres.maxVal;
		} else {
			return mmres.minVal;
		}

		/*
		//Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		//Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
				matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
				255));
		 Rect rec = new Rect(matchLoc_tx,matchLoc_ty);

		*/
	}

	private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
		Mat template = new Mat();
		Mat mROI = mHist.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
				Objdetect.CASCADE_FIND_BIGGEST_OBJECT
						| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
				new Size());

		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length;) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			Rect eye_only_rectangle = new Rect((int) e.tl().x,
					(int) (e.tl().y + e.height * 0.4), (int) e.width,
					(int) (e.height * 0.6));
			mROI = mGray.submat(eye_only_rectangle);
			Mat vyrez = mRgba.submat(eye_only_rectangle);

			
			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

			Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
			iris.x = mmG.minLoc.x + eye_only_rectangle.x;
			iris.y = mmG.minLoc.y + eye_only_rectangle.y;
			eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
					- size / 2, size, size);
			Core.rectangle(mRgba, eye_template.tl(), eye_template.br(),
					new Scalar(255, 0, 0, 255), 2);
			template = (mGray.submat(eye_template)).clone();
			return template;
		}
		return template;
	}


	public void onRecreateClick(View v)
    {
    	learn_frames = 0;
    }

	private void setMinFaceSize(float faceSize) {
		mRelativeFaceSize = faceSize;
		mAbsoluteFaceSize = 0;
	}
}
