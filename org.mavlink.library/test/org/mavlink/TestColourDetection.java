package org.mavlink;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

/**
 * Created by Wesley on 24/04/2016.
 */
public class TestColourDetection {

    static {
        // Load the native OpenCV library
        System.out.println(System.getProperty("java.library.path"));
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }
    
    public static void start() {
		Client client = new Client("169.254.110.196", 55555, data ->{
			System.out.println(data.toString());
		});
		try {
			client.startConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    VideoCapture cap = new VideoCapture(0); //capture the video from webcam
	
        if(!cap.isOpened()) {
            System.out.println("cannot open camera");
            return;
        }

	    //Capture a temporary image from the camera
	 	Mat imgTmp = new Mat();
	 	cap.read(imgTmp); 
	 	int width = imgTmp.width();
	 	int height = imgTmp.height();
        Mat imgOriginal = new Mat( imgTmp.size(), CvType.CV_8UC3 );
	 	Mat imgHSV = new Mat( imgTmp.size(), CvType.CV_8UC3 );
        Mat imgThresholded = new Mat( imgTmp.size(), CvType.CV_8UC3 );
	 
	    while (true) {
	        boolean bSuccess = cap.read(imgOriginal); // read a new frame from video
	        if (!bSuccess) {//if not success, break loop
	        	 System.out.println("Cannot read a frame from video stream");
	             break;
	        }
	        Imgproc.cvtColor(imgOriginal, imgHSV, Imgproc.COLOR_BGR2HSV); //Convert the captured frame from BGR to HSV
	      
	        Core.inRange(imgHSV, new Scalar(0,140,140), new Scalar(10,255,255), imgThresholded);
	        
	        //morphological opening (removes small objects from the foreground)
	        Imgproc.erode(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
	        Imgproc.dilate( imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) ); 
	
	        //morphological closing (removes small holes from the foreground)
	        Imgproc.dilate( imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) ); 
	        Imgproc.erode(imgThresholded, imgThresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)) );
	
	        //see if we can find blobs based on contours
	        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	        Mat hierarchy = new Mat();
	        Imgproc.findContours(imgThresholded, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);;
//	        System.out.println("blob count: " + contours.size());
	        if (contours.size() >= 3) {
	        	List<Entry<Point, Double>> markers = new ArrayList<Entry<Point, Double>>();
	        	for (int i = 0; i < contours.size(); i++) {
	        		Moments moments = Imgproc.moments(contours.get(i));
	        		double dM01 = moments.m01;
	    	        double dM10 = moments.m10;
	    	        double dArea = moments.m00;
	    	        if (dArea > 50) {
	    	        	//calculate the position of the marker
	    	        	int posX = (int) (dM10 / dArea);
	    	        	int posY = (int) (dM01 / dArea);
	    	        	markers.add(new AbstractMap.SimpleEntry<Point,Double>(new Point(posX, posY), dArea));
	    	//        	System.out.println("BLOB " + i + ": x: " + posX + " - y: " + posY + " area: " + dArea);
	    	        } else {
	    	//        	System.out.println("area is only " + dArea);
	    	        }
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
	        	if (actualMarkers.size() == 3) {
//	        		System.out.println("FIRST: " + firstMarker);
//	        		Imgproc.circle(imgOriginal, actualMarkers.get(firstMarker), 2, new Scalar(0,255,0));
	        		double totalX = 0, totalY =0;
	        		for (Point p : actualMarkers) {
	        			totalX += p.x;
	        			totalY += p.y;
	        		}
	        		double avX = totalX / 3;
	        		double avY = totalY / 3;
		  //      	System.out.println("AVERAGE: x: " + avX + " - y: " + avY);
	        		Imgproc.circle(imgOriginal, new Point(avX, avY), 2, new Scalar(0,255,0),5);
	        		
                    int centreX = width/2;
                    int centreY = height/2;
                    double relativeY = avY - centreY;
                    double relativeX = avX - centreX;
//                    System.out.println("Centre - x: " + centreX + ", y: " + centreY + " ----- relative pos of QR - x: " + relativeX + ", y: " + relativeY);
                    double aovHorizontal = Math.toRadians(39); //39
                    double aovVertical = Math.toRadians(22);   //22
                    double pitch = TestMavlinkReader.pitch;// Math.toRadians(0);
                    double roll = TestMavlinkReader.roll;// Math.toRadians(0);
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
					double actualSizeMetres = 0.194;
					double f = (125.8 * 1.00)/actualSizeMetres; //(pixel * distance)/actual - testing shows it appears 110 pixels at distance = 1m (for A3 size)
					double actualDistance = (actualSizeMetres * f) / perceivedPixelLength;
                    System.out.println("Distance is: Pixel: " + perceivedPixelLength + " Actual: " + actualDistance);

                    //now find altitude - swap since camera is at 90 degrees to drone direction
                    double betaY = Math.atan(Math.tan(aovHorizontal) * relativeX / (width/2));
                    double betaX = Math.atan(Math.tan(aovVertical) * relativeY / (height/2));
                    double thetaX = roll + betaX;
                    System.out.println("betaX = " + betaX + ", roll = " + roll + " , thetaX = " + thetaX + " relativeY=" + relativeY);
                    double thetaY = pitch + betaY;
                    System.out.println("betaY = " + betaY + ", pitch = " + pitch + " , thetaY = " + thetaY + " relativeX=" + relativeX);
                    double altitude = Math.sqrt(squared(actualDistance)/(1+squared(Math.tan(thetaX)) + squared(Math.tan(thetaY))));

                    //now find x and y offset
                    double xOffset = altitude * Math.tan(thetaX);
                    double yOffset = altitude * Math.tan(thetaY);
                    
                    //send
	        		try {
						client.send("pos:" + xOffset + ":" + yOffset);
						client.send("alt:" + altitude);
						client.send("dist:"+ actualDistance);
					} catch (Exception e) {
						e.printStackTrace();
					}
	        	}
	        }
	        
//	        //Calculate the moments of the thresholded image
//	        Moments oMoments = Imgproc.moments(imgThresholded);
//	
//	        double dM01 = oMoments.m01;
//	        double dM10 = oMoments.m10;
//	        double dArea = oMoments.m00;
//	
//	        // if the area <= 10000, I consider that the there are no object in the image and it's because of the noise, the area is not zero 
//	        if (dArea > 10000) {
//	        	//calculate the position of the ball
//	        	int posX = (int) (dM10 / dArea);
//	        	int posY = (int) (dM01 / dArea);       
//	        	System.out.println("OVERALL: x: " + posX + " - y: " + posY);
//		        
//	        	if (iLastX >= 0 && iLastY >= 0 && posX >= 0 && posY >= 0) {
//	        		//Draw a red line from the previous point to the current point
////	        		Imgproc.line(imgLines, new Point(posX, posY), new Point(iLastX, iLastY), new Scalar(0,0,255), 2);
//	        	}
//	
//	        	iLastX = posX;
//	        	iLastY = posY;
//	        }
//        	thresholdFrame.render(imgThresholded);//show the thresholded image
//      //  	Core.add(imgOriginal,imgLines, imgOriginal);
//        	imgFrame.render(imgOriginal); //show the original image
	        VideoWriter writer = new VideoWriter("./out.avi", VideoWriter.fourcc('M', 'J', 'P', 'G'),
	        		12, new Size(width,height), true);
	        writer.write(imgOriginal);
	    }
	    return;
	}

	private static double squared(double value) {
		return Math.pow(value, 2);
	}

	private static double distance(Point point, Point point2) {
		return Math.sqrt(Math.pow(point.x-point2.x, 2) + Math.pow(point.y-point2.y, 2));
	}
   
}