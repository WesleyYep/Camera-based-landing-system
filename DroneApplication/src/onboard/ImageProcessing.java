package onboard;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

import network.Client;

/**
 * Created by Wesley on 24/04/2016.
 */
public class ImageProcessing {

    static {
        // Load the native OpenCV library
        System.out.println(System.getProperty("java.library.path"));
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }
    public Client client;
    public boolean isStreaming = false;
    public double hMin, hMax, sMin, sMax, vMin, vMax;
    private boolean isLandingPadFlat = false;
	private boolean isBinary = false;
	public Drone drone;
	public DroneApplication droneApplication;
	private double xOffset;
	private double yOffset;
    private boolean isSmallPattern = false;
	private boolean bigPattern = false;
	private long timeSinceLastSearchOrDetection = System.currentTimeMillis();
	private int snapshotCounter = 0;
	
	public ImageProcessing(Drone drone, DroneApplication app) {
		this.drone = drone;
		this.droneApplication = app;
	}
    
    public void start() {
		Client streamClient = new Client("169.254.110.196", 55556, null);
		try {
			client.startConnection();
			streamClient.startConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    VideoCapture cap = new VideoCapture(0); //capture the video from webcam
	
        if(!cap.isOpened()) {
            System.out.println("cannot open camera");
        }

	    //Capture a temporary image from the camera
        Mat imgTmp = new Mat();
	 	cap.read(imgTmp); 
	 	int width = imgTmp.width();
	 	int height = imgTmp.height();
        Mat imgOriginal = new Mat( imgTmp.size(), CvType.CV_8UC3 );
	 	Mat imgHSV = new Mat( imgTmp.size(), CvType.CV_8UC3 );
        Mat imgThresholded = new Mat( imgTmp.size(), CvType.CV_8UC3 );
        Mat upperRedThresholded = new Mat( imgTmp.size(), CvType.CV_8UC3 );
        Mat hierarchy = new Mat();
        double previousVariance = -1;
 //       System.out.println("width: " + width + " height:" + height);

        try {
			client.send("start");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        System.out.println("Started!");
	    while (true) {
	        boolean bSuccess = cap.read(imgOriginal); // read a new frame from video
	        if (!bSuccess) {//if not success, break loop
	        	 System.out.println("Cannot read a frame from video stream");
	             break;
	        }
	        if (snapshotCounter > 0) {
	    		Imgcodecs.imwrite("snapshot_original_" + System.currentTimeMillis() + ".png", imgOriginal);
	    		snapshotCounter--;
	        }
	        
	        //do circular search if pattern hasn't been detected for 5 seconds
//	        if (System.currentTimeMillis() - timeSinceLastSearchOrDetection > 5000) {
//	     //   	System.out.println("should do search");
//	        	droneApplication.circularSearchInBackground();
//	        	timeSinceLastSearchOrDetection = System.currentTimeMillis();
//	        }
	        
	        Imgproc.cvtColor(imgOriginal, imgHSV, Imgproc.COLOR_RGB2HSV); //Convert the captured frame from BGR to HSV
	      
	        Core.inRange(imgHSV, new Scalar(hMin,sMin,vMin), new Scalar(hMax,sMax,vMax), imgThresholded);
	        
	        //detect second red
	        Core.inRange(imgHSV, new Scalar(160,sMin,vMin), new Scalar(180,sMax,vMax), upperRedThresholded);
	        Core.add(imgThresholded, upperRedThresholded, imgThresholded);
	        
	        if (snapshotCounter > 0) {
	    		Imgcodecs.imwrite("snapshot_threshold_" + System.currentTimeMillis() + ".png", imgThresholded);
	    		snapshotCounter--;
	        }
	        
	        //morphological opening (removes small objects from the foreground)
//	        Imgproc.erode(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
//	        Imgproc.dilate( imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) ); 
//	
//	        //morphological closing (removes small holes from the foreground)
//	        Imgproc.dilate( imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) ); 
//	        Imgproc.erode(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
	        
	        //send binary image here
	        if (isStreaming && isBinary ) {
		        byte[] data = new byte[(int) (width * height * imgThresholded.channels())];
		        imgThresholded.get(0, 0, data);
		        try {
		        	streamClient.sendBytes(data);
				} catch (Exception e) {
					failure(e);
				}
	        }
	        
	        //see if we can find blobs based on contours
	        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	        
	        Imgproc.findContours(imgThresholded, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);;
//	        System.out.println("blob count: " + contours.size());
	        if (contours.size() >= 3) {
	        	List<Entry<Point, Double>> markers = new ArrayList<Entry<Point, Double>>();
	        	for (int i = 0; i < contours.size(); i++) {
	        		Moments moments = Imgproc.moments(contours.get(i));
	        		double dM01 = moments.m01;
	    	        double dM10 = moments.m10;
	    	        double dArea = moments.m00;
	    	        if (dArea > 200/* && dArea < 20000*/) {
	    	        	//calculate the position of the marker
	    	        	int posX = (int) (dM10 / dArea);
	    	        	int posY = (int) (dM01 / dArea);
	    	        	markers.add(new AbstractMap.SimpleEntry<Point,Double>(new Point(posX, posY), dArea));
	    	//        	System.out.println("BLOB " + i + ": x: " + posX + " - y: " + posY + " area: " + dArea);
	    	        } else {
//	    	        	System.out.println("area is only " + dArea);
	    	        }
//	    	        System.out.println("marker size: " + dArea);
	        	}
	        	double min = 999999998;
	        	int firstMarker = 0;  //top-left marker
	        	List<Point> actualMarkers = new ArrayList<Point>();
	        	for (int i = 0; i < markers.size(); i++) {
	        		double first = 0;
	        		double difference = 999999999;
	        		for (int j = 0; j < markers.size(); j++) {
	        			if (i != j) { 
	        				double ratio = distance(markers.get(i).getKey(), markers.get(j).getKey())/(markers.get(i).getValue());
	        				if (ratio < 0.2) {
	        					if (first == 0) {
	        						first = ratio;
	        					} else if (Math.abs(ratio - first) < difference){
	        						difference = Math.abs(ratio - first);
	        						actualMarkers.add(markers.get(i).getKey()); //needs to be 2 other marker with similar distance/area ratios
	        						break;
	        					}
	        				}
        				}
	        		}
	        		if (difference < min) {
	        			firstMarker = actualMarkers.size() - 1;
	        			min = difference;
	        		}
	        	}
	 //       	System.out.println("num: " + actualMarkers.size());
       	
	        	if (actualMarkers.size() > 3) {
	        		TreeMap<Double, Point> distances = new TreeMap<Double, Point>();
	        		for (int i = 0; i < actualMarkers.size(); i++) {
	        			double distance = 0;
	        			for (int j = 0; j < actualMarkers.size(); j++) {
	        				if (i != j) {
	        					distance += distance(actualMarkers.get(i), actualMarkers.get(j));
	        				}
	        			}
	        			distances.put(distance, actualMarkers.get(i));
	        		}
	        		int count = 0;
	        		while (actualMarkers.size() > 3 && count < 100){
	        			actualMarkers.remove(distances.lastEntry().getValue());
	        			distances.remove(distances.lastKey());
	        			count++;
	        			if (count == 100) { System.out.println("too many markers"); }
	        		}
	        	}
	        	if (actualMarkers.size() == 3) {
	        		double totalX = 0, totalY =0;
	        		for (Point p : actualMarkers) {
	        			totalX += p.x;
	        			totalY += p.y;
	        		}
	        		double avX = totalX / 3;
	        		double avY = totalY / 3;
	        		double totalVarX = 0, totalVarY = 0;
	        		for (Point p : actualMarkers) {
	        			totalVarX += squared(p.x-avX);
	        			totalVarY += squared(p.y-avY);
	        		}
	        		double varianceX = totalVarX/3;
	        		double varianceY = totalVarY/3;
	   //     		System.out.println("variance = x:" + varianceX + " , y: " + varianceY);
	        		Imgproc.circle(imgOriginal, new Point(avX, avY), 2, new Scalar(0,255,0),5);
	        		
                    int centreX = width/2;
                    int centreY = height/2;
                    double relativeY = (avY - centreY)*-1; //camera is reversed
                    double relativeX = avX - centreX;
//                    System.out.println("Centre - x: " + centreX + ", y: " + centreY + " ----- relative pos of QR - x: " + relativeX + ", y: " + relativeY);
                    double aovHorizontal = Math.toRadians(33.6); //29 31.1
                    double aovVertical = Math.toRadians(24.5);   //22 24.4
                    double pitch = drone.pitch;// Math.toRadians(0);
                    double roll = drone.roll;// Math.toRadians(0);
//                    double actualX, actualY;
//                    double h = 30; //in m
//                    actualX = h * Math.tan(roll) - (-relativeX/width)*h*(Math.tan(roll+aovHorizontal) - Math.tan(roll-aovHorizontal)); // in m
//                    actualY = h * Math.tan(pitch) - (-relativeY/height)*h*(Math.tan(pitch+aovVertical) - Math.tan(pitch-aovVertical)); // in m
//                    System.out.println("adjusted relative pos is - x: " + actualX + " - y: " + actualY);
                    
                    //now find distance
                    double dist1 = distance(actualMarkers.get(0), actualMarkers.get(1));
                    double dist2 = distance(actualMarkers.get(0), actualMarkers.get(2));
                    double dist3 = distance(actualMarkers.get(1), actualMarkers.get(2));
                    double maxDistance = Math.max(Math.max(dist1,dist2), dist3);

                    double perceivedPixelLength = maxDistance;
			//		double actualSizeMetres = 0.194;
			//		double f = (125.8 * 1.00)/actualSizeMetres; //(pixel * distance)/actual - testing shows it appears 110 pixels at distance = 1m (for A3 size)
			//		double actualDistance = (actualSizeMetres * f) / perceivedPixelLength;
            //        System.out.println("Distance is: Pixel: " + perceivedPixelLength + " Actual: " + actualDistance);
                    double angle = -1;
                    double ratio = -1;
                    //now find angle
                    if (dist1 == maxDistance) { //marker 2 is centre
                    	angle = angleBetween(actualMarkers.get(2), actualMarkers.get(0), actualMarkers.get(1));
                    	ratio = dist2 > dist3 ? dist2/dist3 : dist3/dist2;
                    } else if (dist2 == maxDistance) { //marker 1 is centre
                    	angle = angleBetween(actualMarkers.get(1), actualMarkers.get(0), actualMarkers.get(2));
                    	ratio = dist1 > dist3 ? dist1/dist3 : dist3/dist1;
                    }else if (dist3 == maxDistance) { //marker 0 is centre
                    	angle = angleBetween(actualMarkers.get(0), actualMarkers.get(1), actualMarkers.get(2));                    	
                    	ratio = dist1 > dist2 ? dist1/dist2 : dist2/dist1;
                    }
         //           System.out.println("angle is: " + angle + ", and ratio is: " + ratio);
                    if (!isSmallPattern && previousVariance != -1 && varianceY/previousVariance < 0.2) {
                    	isSmallPattern = true;
                    } else if (isSmallPattern && varianceY/previousVariance > 5) {
                    	isSmallPattern = false;
                    }
                    previousVariance = varianceY;
                    try {
                    	double normalRatio = !isSmallPattern ? 1.06 : 1.06; //based on big landing pad
	                    if (isLandingPadFlat) {
	                    	if (Math.abs(Math.abs(angle)%180-90) > 5 || Math.abs(normalRatio - ratio) > 0.05) {
	                    		client.send("flat:false");
	                    		isLandingPadFlat = false;
	                    	}
	                    } else {
//	                    	System.out.println("angle: " + angle + "   ratio: " + ratio);
	                    	if (Math.abs(Math.abs(angle)%180-90) <= 5 && Math.abs(normalRatio - ratio) < 0.05) {
	                    		client.send("flat:true");
	                    		isLandingPadFlat = true;
	                    	}
	                    }
                    } catch (Exception e) {
                    	failure(e);
                    }
                    
                    //now find altitude - swap since camera is at 90 degrees to drone direction
                    double betaX = Math.atan(Math.tan(aovHorizontal) * relativeX / (width/2));
                    double betaY = Math.atan(Math.tan(aovVertical) * relativeY / (height/2));
                    double thetaX = betaX + roll;
   //                 System.out.println("betaX = " + betaX + ", roll = " + roll + " , thetaX = " + thetaX + " relativeY=" + relativeY);
                    double thetaY = betaY + pitch;
   //                 System.out.println("betaY = " + betaY + ", pitch = " + pitch + " , thetaY = " + thetaY + " relativeX=" + relativeX);
                    //double altitude = Math.sqrt(squared(actualDistance)/(1+squared(Math.tan(thetaX)) + squared(Math.tan(thetaY))));
                    double actualSizeMetres;
                    if (!bigPattern) { //half size pattern used
                    	actualSizeMetres = !isSmallPattern ? 0.1155 : 0.0192; // 0.303 : 0.051
                    } else { //full size pattern used
                    	actualSizeMetres = !isSmallPattern ? 0.303 : 0.051;
                    }
                    double altitude = actualSizeMetres / (Math.tan(perceivedPixelLength*aovHorizontal/640));
                    
                    //now find x and y offset
                    xOffset = altitude * Math.tan(thetaX);
                    yOffset = altitude * Math.tan(thetaY);
                    
                    //set time last detected
                    timeSinceLastSearchOrDetection = System.currentTimeMillis();
                    
                    //send command to drone if it is ready to accept commands
//                    if (droneApplication.isReadyForCommand()) {
//                    	droneApplication.command(xOffset, yOffset);
                    	droneApplication.setOffsetValues(xOffset, yOffset);
//                    }
                    
                    //send
	        		try {
						client.send("pos:" + xOffset + ":" + yOffset);
						client.send("alt:" + altitude);
				//		client.send("dist:"+ actualDistance);
					} catch (Exception e) {
						failure(e);
					}
	        	} else {
	        		droneApplication.setOffsetValues(-1, -1);
	        	}
	        }

	        if (snapshotCounter > 0) {
	    		Imgcodecs.imwrite("snapshot_final_" + System.currentTimeMillis() + ".png", imgOriginal);
	    		Imgcodecs.imwrite("snapshot_contours_" + System.currentTimeMillis() + ".png", imgThresholded);
	    		snapshotCounter--;
	        }
	        
	        if (isStreaming && !isBinary) {
		        byte[] data = new byte[(int) (width * height * imgOriginal.channels())];
		        Imgproc.cvtColor(imgOriginal, imgOriginal, Imgproc.COLOR_RGB2BGR);
		        imgOriginal.get(0, 0, data);
		        try {
		        	streamClient.sendBytes(data);
				} catch (Exception e) {
					failure(e);
				}
	        }
			try {
				client.send("mode:" + drone.currentMode + ":" + drone.currentCustomMode);
			} catch (Exception e) {
				failure(e);
			}
	    }
	    System.out.println("finished");
	    return;
	}
    
    private void failure(Exception e) {
    	e.printStackTrace();
    	//switch to stabilize
    	System.out.println("connection lost! Switching to stabilize mode");
    	droneApplication.changeMode("stabilize", true);
    	System.exit(-1);
    }
    
    private double angleBetween(Point center, Point current, Point previous) {
    	return Math.toDegrees(Math.atan2(current.x - center.x,current.y - center.y)-
                Math.atan2(previous.x- center.x,previous.y- center.y));
	}

	private double squared(double value) {
		return Math.pow(value, 2);
	}

	private double distance(Point point, Point point2) {
		return Math.sqrt(Math.pow(point.x-point2.x, 2) + Math.pow(point.y-point2.y, 2));
	}
	
	public double getXOffset() {
		return xOffset;
	}

	public double getYOffset() {
		return yOffset;
	}
	
	public void setBigPattern(boolean value) {
		bigPattern = value;
		isSmallPattern = false;
		if (bigPattern) {
			System.out.println("Switched to big pattern");
		} else {
			System.out.println("Switched to small pattern");
		}
	}

	public void snapshot() {
		snapshotCounter = 3;
	}
}