package org.mavlink;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley on 24/04/2016.
 */
public class TestQR {

    static int CV_QR_NORTH = 0;
    static int CV_QR_EAST = 1;
    static int CV_QR_SOUTH = 2;
    static int CV_QR_WEST = 3;
    static int width;
    static int height;

    static {
        // Load the native OpenCV library
        System.out.println(System.getProperty("java.library.path"));
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }

    // Start of Main Loop
//------------------------------------------------------------------------------------------------------------------------
    public static void start() {
    //    VideoCapture capture = new VideoCapture("test1.mp4");
        VideoCapture capture = new VideoCapture(0);

     //   MyFrame frame = new MyFrame();
    //    frame.setVisible(true);

        //Mat image = imread(argv[1]);
        Mat image = new Mat();

        if(!capture.isOpened()) {
            System.out.println("cannot open camera");
            return;
        }

        //Step	: Capture a frame from Image Input for creating and initializing manipulation variables
        //Info	: Inbuilt functions from OpenCV
        //Note	:

        capture.read(image);
        if(image.empty()){
            System.out.println("ERR: Unable to query image from capture device.");
            return;
        }
        width = image.width();
        height = image.height();


     //   Mat qr,qr_raw,qr_gray,qr_thres;

        int key = 0;
        while(key != 'q')				// While loop to query for Image Input frame
        {
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            MatOfInt4 hierarchy = new MatOfInt4();

            int mark=0,A=0,B=0,C=0,top=0,right=0,bottom=0,median1=0,median2=0,outlier=0;
            double AB;
            double BC;
            double CA;
            double dist;
            double slope;

            int align=0,orientation=0;

            int DBG=1;						// Debug Flag
            // Creation of Intermediate 'Image' Objects required later
            Mat gray = new Mat(image.height(),image.width(), CvType.CV_8UC1); // To hold Grayscale Image
            Mat edges = new Mat(image.height(),image.width(), CvType.CV_8UC1); // To hold Grayscale Image
            Mat traces = new Mat(image.size(),  CvType.CV_8UC3);			    // For Debug Visuals

            capture.read(image);						// Capture Image from Image Input
            Imgproc.cvtColor(image,gray, Imgproc.COLOR_RGB2GRAY);		// Convert Image captured from Image Input to GrayScale
            Imgproc.Canny(gray, edges, 100 , 200, 3, false);		// Apply Canny edge detection on the gray image
            //could try true ^

            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE); // Find contours with hierarchy

           // System.out.println("contour size: " + contours.size());
            mark = 0;								// Reset all detected marker count for this frame

            // Get Moments for all Contours and the mass centers
            List<Moments> mu = new ArrayList<Moments>();
//            MatOfPoint2f mc = new MatOfPoint2f(contours.size());
            List<Point> mc = new ArrayList<Point>();

            for( int i = 0; i < contours.size(); i++ )
            {
                //mu.set(i, new Moments( contours.get(i), false );
                mu.add(i, Imgproc.moments(contours.get(i), false));
                mc.add(i, new Point( mu.get(i).m10/mu.get(i).m00 , mu.get(i).m01/mu.get(i).m00 ));
            }

            // Start processing the contour data

            // Find Three repeatedly enclosed contours A,B,C
            // NOTE: 1. Contour enclosing other contours is assumed to be the three Alignment markings of the QR code.
            // 2. Alternately, the Ratio of areas of the "concentric" squares can also be used for identifying base Alignment markers.
            // The below demonstrates the first method
        //    System.out.println("dump: " + hierarchy.dump()+ " rows: " + hierarchy.rows());
            for( int i = 0; i < contours.size(); i++ )
            {
                int k=i;
                int c=0;

                while(hierarchy.get(0,k) != null && hierarchy.get(0,k)[2] != -1)
                {
                    k = (int) hierarchy.get(0,k)[2];
                    c = c+1;
                }
               if(hierarchy.get(0,k) != null && hierarchy.get(0,k)[2] != -1)
                    c = c+1;

                if (c >= 5)
                {
                    if (mark == 0)		A = i;
                    else if  (mark == 1)	B = i;		// i.e., A is already found, assign current contour to B
                    else if  (mark == 2)	C = i;		// i.e., A and B are already found, assign current contour to C
                    mark = mark + 1 ;
                }
            }
          //  System.out.println("markers: " + mark);

            // if (mark >= 2)		// Ensure we have (atleast 3; namely A,B,C) 'Alignment Markers' discovered
            if (mark == 3)		// Ensure we have (atleast 3; namely A,B,C) 'Alignment Markers' discovered
            {
                // We have found the 3 markers for the QR code; Now we need to determine which of them are 'top', 'right' and 'bottom' markers

                // Determining the 'top' marker
                // Vertex of the triangle NOT involved in the longest side is the 'outlier'

                AB = cv_distance(mc.get(A),mc.get(B));
                BC = cv_distance(mc.get(B),mc.get(C));
                CA = cv_distance(mc.get(C),mc.get(A));

                if ( AB > BC && AB > CA )
                {
                    outlier = C; median1=A; median2=B;
                }
                else if ( CA > AB && CA > BC )
                {
                    outlier = B; median1=A; median2=C;
                }
                else if ( BC > AB && BC > CA )
                {
                    outlier = A;  median1=B; median2=C;
                }

                top = outlier;							// The obvious choice

                dist = cv_lineEquation(mc.get(median1), mc.get(median2), mc.get(outlier));	// Get the Perpendicular distance of the outlier from the longest side
                double[] temp = cv_lineSlope(mc.get(median1), mc.get(median2),align);		// Also calculate the slope of the longest side
                slope = temp[0];
                align = (int) temp[1];
                // Now that we have the orientation of the line formed median1 & median2 and we also have the position of the outlier w.r.t. the line
                // Determine the 'right' and 'bottom' markers

                if (align == 0)
                {
                    bottom = median1;
                    right = median2;
                }
                else if (slope < 0 && dist < 0 )		// Orientation - North
                {
                    bottom = median1;
                    right = median2;
                    orientation = CV_QR_NORTH;
                }
                else if (slope > 0 && dist < 0 )		// Orientation - East
                {
                    right = median1;
                    bottom = median2;
                    orientation = CV_QR_EAST;
                }
                else if (slope < 0 && dist > 0 )		// Orientation - South
                {
                    right = median1;
                    bottom = median2;
                    orientation = CV_QR_SOUTH;
                }

                else if (slope > 0 && dist > 0 )		// Orientation - West
                {
                    bottom = median1;
                    right = median2;
                    orientation = CV_QR_WEST;
                }


                // To ensure any unintended values do not sneak up when QR code is not present
                float area_top,area_right, area_bottom;

                if( top < contours.size() && right < contours.size() && bottom < contours.size() && Imgproc.contourArea(contours.get(top)) > 10 && Imgproc.contourArea(contours.get(right)) > 10 && Imgproc.contourArea(contours.get(bottom)) > 10 )
                {

                    List<Point> L = new ArrayList<Point>(),M = new ArrayList<Point>(),O = new ArrayList<Point>(), tempL = new ArrayList<Point>(),tempM = new ArrayList<Point>(),tempO = new ArrayList<Point>();
                    PointByRef N = new PointByRef();

                    List<Point> src = new ArrayList<Point>(),dst = new ArrayList<Point>();		// src - Source Points basically the 4 end co-ordinates of the overlay image

                    cv_getVertices(contours,top,slope,tempL);
                    cv_getVertices(contours,right,slope,tempM);
                    cv_getVertices(contours,bottom,slope,tempO);

                    cv_updateCornerOr(orientation, tempL, L); 			// Re-arrange marker corners w.r.t orientation of the QR code
                    cv_updateCornerOr(orientation, tempM, M); 			// Re-arrange marker corners w.r.t orientation of the QR code
                    cv_updateCornerOr(orientation, tempO, O); 			// Re-arrange marker corners w.r.t orientation of the QR code

                    boolean iflag = getIntersectionPoint(M.get(1),M.get(3),L.get(0),L.get(2),N);
                    

               //     System.out.println("src size: " + src.size() + " -  dest size: " + dst.size());
                    if (!iflag) {
                        continue;
                    }
                    Imgproc.circle(image, N.value, 10, new Scalar(0,255,255), 1, 8, 0 );
                    int centreX = width/2;
                    int centreY = height/2;
                    double relativeY = N.value.y - centreY;
                    double relativeX = N.value.x - centreX;
                    System.out.println("Centre - x: " + centreX + ", y: " + centreY + " ----- relative pos of QR - x: " + relativeX + ", y: " + relativeY);
                    double aovHorizontal = Math.toRadians(54);
                    double aovVertical = Math.toRadians(41);
                    double pitch = TestMavlinkReader.pitch;// Math.toRadians(0);
                    double roll = TestMavlinkReader.roll;// Math.toRadians(0);
                    double actualX, actualY;
                    double h = 30; //in m
                    actualX = h * Math.tan(roll) - (-relativeX/width)*h*(Math.tan(roll+aovHorizontal) - Math.tan(roll-aovHorizontal)); // in m
                    actualY = h * Math.tan(pitch) - (-relativeY/height)*h*(Math.tan(pitch+aovVertical) - Math.tan(pitch-aovVertical)); // in m
                    System.out.println("adjusted relative x is: " + actualX);	
                    System.out.println("adjusted relative y is: " + actualY);	
                    
                    //Draw contours on the image
                    if (DBG == 1) {
                    	Imgproc.drawContours(image, contours, top, new Scalar(255, 200, 0), 2, 8, hierarchy, 0, new Point());
                    	Imgproc.drawContours(image, contours, right, new Scalar(0, 0, 255), 2, 8, hierarchy, 0, new Point());
                    	Imgproc.drawContours(image, contours, bottom, new Scalar(255, 0, 100), 2, 8, hierarchy, 0, new Point());
                    }
                    // Insert Debug instructions here
                    if(DBG==1)
                    {
                        // Debug Prints
                        // Visualizations for ease of understanding
                        if (slope > 5)
                            Imgproc.circle( traces, new Point(10,20) , 5 , new Scalar(0,0,255), -1, 8, 0 );
                        else if (slope < -5)
                            Imgproc.circle( traces, new Point(10,20) , 5 , new Scalar(255,255,255), -1, 8, 0 );

                        // Draw contours on Trace image for analysis
                        Imgproc.drawContours( traces, contours, top , new Scalar(255,0,100), 1, 8, hierarchy, 0 , new Point());
                        Imgproc.drawContours( traces, contours, right , new Scalar(255,0,100), 1, 8, hierarchy, 0, new Point() );
                        Imgproc.drawContours( traces, contours, bottom , new Scalar(255,0,100), 1, 8, hierarchy, 0, new Point() );

                        // Draw points (4 corners) on Trace image for each Identification marker
                        Imgproc.circle( traces, L.get(0), 2, new Scalar(255,255,0), -1, 8, 0 );
                        Imgproc.circle( traces, L.get(1), 2, new Scalar(0,255,0), -1, 8, 0 );
                        Imgproc.circle( traces, L.get(2), 2, new Scalar(0,0,255), -1, 8, 0 );
                        Imgproc.circle( traces, L.get(3), 2, new Scalar(128,128,128), -1, 8, 0 );

                        Imgproc.circle( traces, M.get(0), 2, new Scalar(255,255,0), -1, 8, 0 );
                        Imgproc.circle( traces, M.get(1), 2, new Scalar(0,255,0), -1, 8, 0 );
                        Imgproc.circle( traces, M.get(2), 2, new Scalar(0,0,255), -1, 8, 0 );
                        Imgproc.circle( traces, M.get(3), 2, new Scalar(128,128,128), -1, 8, 0 );

                        Imgproc.circle( traces, O.get(0), 2, new Scalar(255,255,0), -1, 8, 0 );
                        Imgproc.circle( traces, O.get(1), 2, new Scalar(0,255,0), -1, 8, 0 );
                        Imgproc.circle( traces, O.get(2), 2, new Scalar(0,0,255), -1, 8, 0 );
                        Imgproc.circle( traces, O.get(3), 2, new Scalar(128,128,128), -1, 8, 0 );

                        // Draw point of the estimated 4th Corner of (entire) QR Code
                        Imgproc.circle( traces, N.value, 2, new Scalar(255,255,255), -1, 8, 0 );

                        // Draw the lines used for estimating the 4th Corner of QR Code
                        Imgproc.line(traces,M.get(1),N.value,new Scalar(0,0,255),1,8,0);
                        Imgproc.line(traces,L.get(0),N.value,new Scalar(0,0,255),1,8,0);

                        // Show the Orientation of the QR Code wrt to 2D Image Space
                     //   int fontFace = FONT_HERSHEY_PLAIN;
                        int fontFace = 1;

                        if(orientation == CV_QR_NORTH)
                        {
                            Imgproc.putText(traces, "NORTH", new Point(20,30), fontFace, 1, new Scalar(0, 255, 0), 1, 8, false);
                            //System.out.println("North");
                        }
                        else if (orientation == CV_QR_EAST)
                        {
                            Imgproc.putText(traces, "EAST", new Point(20,30), fontFace, 1, new Scalar(0, 255, 0), 1, 8, false);
                            //System.out.println("East");
                        }
                        else if (orientation == CV_QR_SOUTH)
                        {
                            //System.out.println("South");
                            Imgproc.putText(traces, "SOUTH", new Point(20,30), fontFace, 1, new Scalar(0, 255, 0), 1, 8, false);
                        }
                        else if (orientation == CV_QR_WEST)
                        {
                            //System.out.println("West");
                            Imgproc.putText(traces, "WEST", new Point(20,30), fontFace, 1, new Scalar(0, 255, 0), 1, 8, false); //try false?
                        }

                        // Debug Prints
                    }

                }
            }

            if (image.rows() > 0) {
    //            frame.render(image);
            } else {
                System.out.println("No captured frame -- camera disconnected");
                break;
            }

            //todo
//            imshow ( "Traces", traces );
//            imshow ( "QR code", qr_thres );

        }	// End of 'while' loop

        return;
    }

// End of Main Loop
//--------------------------------------------------------------------------------------


// Routines used in Main loops

// Function: Routine to get Distance between two points
// Description: Given 2 points, the function returns the distance

    static double cv_distance(Point P, Point Q)
    {
        return Math.sqrt(Math.pow(Math.abs(P.x - Q.x),2) + Math.pow(Math.abs(P.y - Q.y),2)) ;
    }


// Function: Perpendicular Distance of a Point J from line formed by Points L and M; Equation of the line ax+by+c=0
// Description: Given 3 points, the function derives the line quation of the first two points,
//	  calculates and returns the perpendicular distance of the the 3rd point from this line.

    static double cv_lineEquation(Point L, Point M, Point J)
    {
        double a,b,c,pdist;

        a = -((M.y - L.y) / (M.x - L.x));
        b = 1.0;
        c = (((M.y - L.y) /(M.x - L.x)) * L.x) - L.y;

        // Now that we have a, b, c from the equation ax + by + c, time to substitute (x,y) by values from the Point J

        pdist = (a * J.x + (b * J.y) + c) / Math.sqrt((a * a) + (b * b));
        return pdist;
    }

// Function: Slope of a line by two Points L and M on it; Slope of line, S = (x1 -x2) / (y1- y2)
// Description: Function returns the slope of the line formed by given 2 points, the alignement flag
//	  indicates the line is vertical and the slope is infinity.

    static double[] cv_lineSlope(Point L, Point M, int alignment)
    {
        double dx,dy;
        dx = M.x - L.x;
        dy = M.y - L.y;

        if ( dy != 0)
        {
            alignment = 1;
            return new double[]{(dy / dx), alignment};
        }
        else				// Make sure we are not dividing by zero; so use 'alignement' flag
        {
            alignment = 0;
            return new double[] { 0.0, alignment};
        }
    }



    // Function: Routine to calculate 4 Corners of the Marker in Image Space using Region partitioning
// Theory: OpenCV Contours stores all points that describe it and these points lie the perimeter of the polygon.
//	The below function chooses the farthest points of the polygon since they form the vertices of that polygon,
//	exactly the points we are looking for. To choose the farthest point, the polygon is divided/partitioned into
//	4 regions equal regions using bounding box. Distance algorithm is applied between the centre of bounding box
//	every contour point in that region, the farthest point is deemed as the vertex of that region. Calculating
//	for all 4 regions we obtain the 4 corners of the polygon ( - quadrilateral).
    static void cv_getVertices(List<MatOfPoint> contours, int c_id, double slope, List<Point> quad)
    {
        Rect box;
        box = Imgproc.boundingRect( contours.get(c_id));

        PointByRef M0 = new PointByRef();
        PointByRef M1 = new PointByRef();
        PointByRef M2 = new PointByRef();
        PointByRef M3 = new PointByRef();
        Point A = new Point();
        Point B = new Point();
        Point C = new Point();
        Point D = new Point();
        Point W = new Point();
        Point X = new Point();
        Point Y = new Point();
        Point Z = new Point();

        A =  box.tl();
        B.x = box.br().x;
        B.y = box.tl().y;
        C = box.br();
        D.x = box.tl().x;
        D.y = box.br().y;


        W.x = (A.x + B.x) / 2;
        W.y = A.y;

        X.x = B.x;
        X.y = (B.y + C.y) / 2;

        Y.x = (C.x + D.x) / 2;
        Y.y = C.y;

        Z.x = D.x;
        Z.y = (D.y + A.y) / 2;

        DoubleByRef[] dmax = new DoubleByRef[4];
        dmax[0] = new DoubleByRef();
        dmax[1] = new DoubleByRef();
        dmax[2] = new DoubleByRef();
        dmax[3] = new DoubleByRef();
        dmax[0].value=0.0;
        dmax[1].value=0.0;
        dmax[2].value=0.0;
        dmax[3].value=0.0;

        double pd1 = 0.0;
        double pd2 = 0.0;

        if (slope > 5 || slope < -5 )
        {

            for( int i = 0; i < contours.get(c_id).toArray().length; i++ )
            {
                pd1 = cv_lineEquation(C,A,contours.get(c_id).toArray()[i]);	// Position of point w.r.t the diagonal AC
                pd2 = cv_lineEquation(B,D,contours.get(c_id).toArray()[i]);	// Position of point w.r.t the diagonal BD

                if((pd1 >= 0.0) && (pd2 > 0.0))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],W,dmax[1],M1);
                }
                else if((pd1 > 0.0) && (pd2 <= 0.0))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],X,dmax[2],M2);
                }
                else if((pd1 <= 0.0) && (pd2 < 0.0))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],Y,dmax[3],M3);
                }
                else if((pd1 < 0.0) && (pd2 >= 0.0))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],Z,dmax[0],M0);
                }
                else
                    continue;
            }
        }
        else
        {
            int halfx = (int) ((A.x + B.x) / 2);
            int halfy = (int) ((A.y + D.y) / 2);

            for( int i = 0; i < contours.get(c_id).toArray().length; i++ )
            {
                if((contours.get(c_id).toArray()[i].x < halfx) && (contours.get(c_id).toArray()[i].y <= halfy))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],C,dmax[2],M0);
                }
                else if((contours.get(c_id).toArray()[i].x >= halfx) && (contours.get(c_id).toArray()[i].y < halfy))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],D,dmax[3],M1);
                }
                else if((contours.get(c_id).toArray()[i].x > halfx) && (contours.get(c_id).toArray()[i].y >= halfy))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],A,dmax[0],M2);
                }
                else if((contours.get(c_id).toArray()[i].x <= halfx) && (contours.get(c_id).toArray()[i].y > halfy))
                {
                    cv_updateCorner(contours.get(c_id).toArray()[i],B,dmax[1],M3);
                }
            }
        }

        quad.add(M0.value);
        quad.add(M1.value);
        quad.add(M2.value);
        quad.add(M3.value);

    }

    // Function: Compare a point if it more far than previously recorded farthest distance
// Description: Farthest Point detection using reference point and baseline distance
    static void cv_updateCorner(Point P, Point ref , DoubleByRef baseline,  PointByRef corner)
    {
        double temp_dist;
        temp_dist = cv_distance(P,ref);

        if(temp_dist > baseline.value)
        {
            baseline.value = temp_dist;			// The farthest distance is the new baseline
            corner.value = P;						// P is now the farthest point
        }

    }

    // Function: Sequence the Corners wrt to the orientation of the QR Code
    static void cv_updateCornerOr(int orientation, List<Point> IN, List<Point> OUT)
    {
        Point M0 = null,M1 = null,M2 = null,M3 = null;
        if(orientation == CV_QR_NORTH)
        {
            M0 = IN.get(0);
            M1 = IN.get(1);
            M2 = IN.get(2);
            M3 = IN.get(3);
        }
        else if (orientation == CV_QR_EAST)
        {
            M0 = IN.get(1);
            M1 = IN.get(2);
            M2 = IN.get(3);
            M3 = IN.get(0);
        }
        else if (orientation == CV_QR_SOUTH)
        {
            M0 = IN.get(2);
            M1 = IN.get(3);
            M2 = IN.get(0);
            M3 = IN.get(1);
        }
        else if (orientation == CV_QR_WEST)
        {
            M0 = IN.get(3);
            M1 = IN.get(0);
            M2 = IN.get(1);
            M3 = IN.get(2);
        }

        OUT.add(M0);
        OUT.add(M1);
        OUT.add(M2);
        OUT.add(M3);
    }

    // Function: Get the Intersection Point of the lines formed by sets of two points
    static boolean getIntersectionPoint(Point a1, Point a2, Point b1, Point b2, PointByRef intersection)
    {
    	if (a1 == null || a2 == null || b1 == null || b2 == null || intersection == null) {
    		return false;
    	}
    	double x12 = a1.x - a2.x;
    	double x34 = b1.x - b2.x;
    	double y12 = a1.y - a2.y;
    	double y34 = b1.y - b2.y;
    	double c = x12 * y34 - y12 * x34;

    	if (Math.abs(c) < 0.01)
    	{
    	  // No intersection
    	  return false;
    	}
    	else
    	{
    	  // Intersection
    		double a = a1.x * a2.y - a1.y * a2.x;
    		double b = b1.x * b2.y - b1.y * b2.x;

    		intersection.value = new Point((a * x34 - b * x12) / c, (a * y34 - b * y12) / c);

    		return (intersection.value.x > 0 && intersection.value.x < width && intersection.value.y > 0 && intersection.value.y < height)  ;
    	}

    }

    static double cross(Point v1,Point v2)
    {
        return v1.x*v2.y - v1.y*v2.x;
    }

// EOF

}

class DoubleByRef { public double value; }
class PointByRef { public Point value; }