package ui.map;
//******************************************************************************
//This simple sample demonstrates how to route between two locations without
//utilizing MapQuest's server side session management.  To utilize MapQuest's
//server side session management, please view the routeItWithServerSession sample.
//
//For a complete explanation of all objects and their functionality please refer
//to the API documentation located in the mq\clients\java\ directory or
//http://support.mapquest.com.
//
//NOTE: These samples are designed to work with sample data sets, so pool names
//ports and server IPs may need to be modified for your use.
//******************************************************************************

import com.mapquest.*;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.*;
import java.io.*;

//public class RouteItNoServerSession
//{
//   public static void main(String[] args)
//   {
//      DisplayFrameNoServer frame = new DisplayFrameNoServer();
//      frame.addWindowListener(new WindowAdapter()
//      {
//         public void windowClosing(WindowEvent e)
//         {
//            System.exit(0);
//         }
//      });
//      frame.show();
//   }//end main
//}//end class RouteItNoServerSession

class DisplayFrameNoServer extends JFrame
{
   public static final int DEFAULT_WIDTH = 710;
   public static final int DEFAULT_HEIGHT = 740;
   private Vector rowVector;
   private Vector columnNames;
   private Vector dataVector;
   private DefaultTableModel model;
   private DecimalFormat df;
   private String mapURL;
   private int imageHeight;
   private int imageWidth;

/*------------  This method is for java display purposes. ------------*/
   public DisplayFrameNoServer()
   {
//      setTitle("RouteItNoServerSession Results");
//      setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
//
//      //This calls the method which sets the information for obtaining the map
//      doMapQuest();
//
//      //Creates a button to end the program and close the frame
//      JButton button = new JButton("Close");
//      button.addActionListener(new ActionListener()
//      {
//         public void actionPerformed(ActionEvent e)
//         {
//            System.exit(0);
//         }
//      });
//
//      Box buttonBox = Box.createHorizontalBox();
//      buttonBox.add(button);
//
//      //create and image panel to display the map
//      ImagePanelNoServer panel = new ImagePanelNoServer(mapURL, imageWidth, imageHeight);
//
//      //add the image panel to the box layout
//      Box imageBox = Box.createVerticalBox();
//      imageBox.add(Box.createVerticalStrut(10));
//      imageBox.add(panel);
//
//      //create a JTextArea to display the URL
//      JTextArea urlText = new JTextArea("Image URL: " + mapURL);
//      urlText.setPreferredSize(new Dimension(700, 130));
//      urlText.setMaximumSize(new Dimension(800, 130));
//      urlText.setMinimumSize(new Dimension(600, 60));
//      urlText.setEditable(false);
//      urlText.setLineWrap(true);
//      urlText.setWrapStyleWord(false);
//
//      //add the URL to a JScrollPane
//      JScrollPane scrollPane = new JScrollPane(urlText,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//      scrollPane.setMinimumSize(new Dimension(40, 40));
//      scrollPane.setMaximumSize(new Dimension(800, 50));
//
//      //add the JScrollPane containing URL to a box layout
//      Box urlBox = Box.createVerticalBox();
//      urlBox.add(Box.createVerticalStrut(10));
//      urlBox.add(scrollPane);
//
//      //create a table holding direction, distance, and time
//      model = new DefaultTableModel(dataVector, columnNames);
//      JTable table = new JTable(model);
//
//      table.setPreferredScrollableViewportSize(new Dimension(675, 195));
//
//      // Set column widths
//      table.getColumnModel().getColumn(0).setPreferredWidth(85);
//      table.getColumnModel().getColumn(1).setPreferredWidth(375);
//      table.getColumnModel().getColumn(2).setPreferredWidth(125);
//      table.getColumnModel().getColumn(3).setPreferredWidth(115);
//      // end setColumnWidths
//
//      //add the table to a JScrollPane
//      JScrollPane scrollPane2 = new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//      scrollPane2.setMinimumSize(new Dimension(20, 20));
//      scrollPane2.setMaximumSize(table.getPreferredScrollableViewportSize());
//
//      //add the JScrollPane containing the table to the box layout
//      Box tableBox = Box.createVerticalBox();
//      tableBox.add(scrollPane2);
//
//      Box displayBox = Box.createVerticalBox();
//
//      displayBox.add(Box.createVerticalStrut(10));
//      displayBox.add(buttonBox);
//      displayBox.add(imageBox);
//      displayBox.add(urlBox);
//      displayBox.add(tableBox);
//
//      //add the boxes to the ContentPane
//      Container contentPane = getContentPane();
//      contentPane.add(displayBox);
   }//end DisplayFrame
/*---------------  End code for java display purposes. ---------------*/

/*-------  This code demonstrates use of the MapQuest server.  -------*/
   public LatLngCollection doMapQuest()
   {
      //MapQuest.Exec is the MapQuest client object.
      //All server requests, such as Geocode and Search, are part of the Exec object.

      Exec geocodeClient = new Exec();
      Exec mapClient = new Exec();
      Exec routeClient = new Exec();

      //Client.ServerName refers to the name of the server where the MapQuest server resides.
      //Client.ServerPath refers to the virtual directory where the MapQuest server resides.
      //Client.ServerPort refers to the port the client uses to communicate with the MapQuest
      mapClient.setServerName(MQServers.MQ_MAP_SERVER_NAME);
      mapClient.setServerPath(MQServers.MQ_MAP_SERVER_PATH);
      mapClient.setServerPort(MQServers.MQ_MAP_SERVER_PORT);
      mapClient.setClientId (MQServers.MQ_MAP_SERVER_CLIENT_ID);
      mapClient.setPassword (MQServers.MQ_MAP_SERVER_PASSWORD);

      //Client.ServerName refers to the name of the server where the MapQuest server resides.
      //Client.ServerPath refers to the virtual directory where the MapQuest server resides.
      //Client.ServerPort refers to the port the client uses to communicate with the MapQuest
      geocodeClient.setClientId (MQServers.MQ_GEOCODE_SERVER_CLIENT_ID);
      geocodeClient.setPassword (MQServers.MQ_GEOCODE_SERVER_PASSWORD);
      geocodeClient.setServerName(MQServers.MQ_GEOCODE_SERVER_NAME);
      geocodeClient.setServerPath(MQServers.MQ_GEOCODE_SERVER_PATH);
      geocodeClient.setServerPort(MQServers.MQ_GEOCODE_SERVER_PORT);

      //Client.ServerName refers to the name of the server where the MapQuest server resides.
      //Client.ServerPath refers to the virtual directory where the MapQuest server resides.
      //Client.ServerPort refers to the port the client uses to communicate with the MapQuest
      routeClient.setClientId (MQServers.MQ_ROUTE_SERVER_CLIENT_ID);
      routeClient.setPassword (MQServers.MQ_ROUTE_SERVER_PASSWORD);
      routeClient.setServerName(MQServers.MQ_ROUTE_SERVER_NAME);
      routeClient.setServerPath(MQServers.MQ_ROUTE_SERVER_PATH);
      routeClient.setServerPort(MQServers.MQ_ROUTE_SERVER_PORT);

      // Create Input Address
      Address address = new Address();

      //Fill in origin information
      address.init();
      address.setStreet("100 Penn St");
      address.setCity("Pittsburgh");
      address.setState("PA");
      address.setPostalCode("");
      address.setCountry("US");

      // Create LocationCollection for results
      LocationCollection lcOriginResults = new LocationCollection();

      GeoAddress gaOrigin = new GeoAddress();

      //Geocode origin location
      try
      {
         geocodeClient.geocode(address,lcOriginResults);
      }
      catch (Exception e)
      {
         JOptionPane.showMessageDialog(this, " Exception: " + e,
         "Exception", JOptionPane.ERROR_MESSAGE);
      }

      //If the results collection's Count property is zero, no matches could be found for the location
      if (lcOriginResults.getSize() != 0)
      {
         try
         {
            gaOrigin = (GeoAddress)lcOriginResults.getAt(0);
         }
         catch(Exception e)
         {
            JOptionPane.showMessageDialog(this, " Exception: " + e,
            "Exception", JOptionPane.ERROR_MESSAGE);
         }
      }
      else
      {
         System.out.println ("The origin could not be geocoded.");
      }


      //Fill in destination information
      address.init();
      address.setStreet("2015 Saw Mill Run Blvd");
      address.setCity("Pittsburgh");
      address.setState("PA");
      address.setPostalCode("");
      address.setCountry("US");

      LocationCollection lcDestResults = new LocationCollection();

      //Geocode destination location
      try
      {
         geocodeClient.geocode(address,lcDestResults);
      }
      catch (Exception e)
      {
         JOptionPane.showMessageDialog(this, " Exception: " + e,
         "Exception", JOptionPane.ERROR_MESSAGE);
      }

      GeoAddress gaDest = new GeoAddress();

      //If the results collection's Count property is zero, no matches could be found for the location
      if (lcDestResults != null)
      {
         try
         {
            gaDest = (GeoAddress)lcDestResults.getAt(0);
         }
         catch(Exception e)
         {
            JOptionPane.showMessageDialog(this, " Exception: " + e,
            "Exception", JOptionPane.ERROR_MESSAGE);
         }
      }
      else
      {
         System.out.println("Destination could not be geocoded.");
      }

      //This is the collection that will hold the geocoded locations to be utilized in the call to DoRoute.
      LocationCollection lcRouteLocations = new LocationCollection();

      lcRouteLocations.add(gaOrigin);
      lcRouteLocations.add(gaDest);

      //Routes may be customized (i.e., shortest or fastest route, whether to avoid
      //toll roads or limited access highways, etc.).  In this sample the number of
      //shape points per maneuver is being set, which provides the detail of the
      //highlight to be drawn. In addition, the CoverageName is also being set, which
      //must match a configured route on the MapQuest server.

      //The RouteOptions object contains information pertaining to the Route to be performed.
      RouteOptions routeOptions = new RouteOptions();

      routeOptions.setMaxShapePointsPerManeuver(500);

      //The RouteResults object will contain the results of the DoRoute call.  The
      //results contains information such as the narrative, drive time and distance.
      RouteResults routeResults = new RouteResults();

      try
      {
         //This call to the server actually generates the route.
         routeClient.doRoute(lcRouteLocations, routeOptions, routeResults, null);
      }
      catch (Exception e)
      {
         JOptionPane.showMessageDialog(this, " Exception: " + e,
         "Exception", JOptionPane.ERROR_MESSAGE);
      }

      //To see a demonstration of the error handling comment out the above line:
      //lcRouteLocations.add(gaDest)
      if (!(routeResults.getResultCode().equals(RouteResultsCode.SUCCESS)))
      {
         int x;
         try
         {
            for (x = 0; x < routeResults.getResultMessages().getSize(); x++)
            System.out.println(routeResults.getResultMessages().getAt(x));
         }
         catch (Exception e)
         {
            JOptionPane.showMessageDialog(this, " Exception: " + e,
            "Exception", JOptionPane.ERROR_MESSAGE);
         }
      }

      //Create DisplayTypes and pointFeatures for the origin and destination locations
      //to be displayed.  For details regarding DisplayTypes and PointFeatures, see the
      //mapWithPoi sample.
      DTStyle originDTStyle = new DTStyle();

      originDTStyle.setDT(3073);
      originDTStyle.setSymbolType(SymbolType.RASTER);
      originDTStyle.setSymbolName("MQ09191");
      originDTStyle.setVisible(true);
      originDTStyle.setLabelVisible(false);


      DTStyle destDTStyle = new DTStyle();

      destDTStyle.setDT(3074);
      destDTStyle.setSymbolType(SymbolType.RASTER);
      destDTStyle.setSymbolName("MQ09192");
      destDTStyle.setVisible(true);
      destDTStyle.setLabelVisible(false);

      CoverageStyle coverageStyle = new CoverageStyle();

      coverageStyle.add(originDTStyle);
      coverageStyle.add(destDTStyle);

      PointFeature ptfOrigin = new PointFeature();
      ptfOrigin.setDT(3073);
      ptfOrigin.setCenterLatLng(gaOrigin.getLatLng());

      PointFeature ptfDest = new PointFeature();
      ptfDest.setDT(3074);
      ptfDest.setCenterLatLng(gaDest.getLatLng());

      FeatureCollection fcRouteNodes = new FeatureCollection();
      fcRouteNodes.add(ptfOrigin);
      fcRouteNodes.add(ptfDest);
      //End origin and destination point generation

      //Create the MapState object
//      MapState mapState = new MapState();
//      mapState.setWidthPixels(450);
//      imageWidth = mapState.getWidthPixels();
//      mapState.setHeightPixels(300);
//      imageHeight = mapState.getHeightPixels();
//      /*The MapScale property tells the server the scale at which to display the map.
//      Level of detail displayed varies depending on the scale of the map.*/
//      mapState.setMapScale(48000);
//      /*Specify the latitude/longitude coordinate to center the map.*/
//      mapState.setCenter(new LatLng(40.44569, -79.890393));


      //Create the Session object.
//      Session mqSession = new Session();
//      mqSession.addOne(mapState);
//      mqSession.addOne(fcRouteNodes);
//      mqSession.addOne(coverageStyle);

      //Create the routeHighlight to be displayed on the map using the LinePrimitive
      //object.  This object allows you to insert lines of your own creation into the map.
//      LinePrimitive lpRtHlt = new LinePrimitive();
//      lpRtHlt.setColor(ColorStyle.GREEN);
//      lpRtHlt.setKey("RouteShape");
//      lpRtHlt.setStyle(PenStyle.SOLID);
//      lpRtHlt.setCoordinateType(CoordinateType.GEOGRAPHIC);
//      lpRtHlt.setDrawTrigger(DrawTrigger.AFTER_ROUTE_HIGHLIGHT);
//      lpRtHlt.setWidth(200);

      //The Generalize method reduces the amount of shape points used to represent a line.
      //This example combines all shape points within .01 miles of each other to 1 single
      //shape point.  This removes unnecessary shape points and shortens the URL to minimize
      //the possibility of exceeding some browsers' URL size limitations.  There may still
      //be cases where URL length is an issue, in which case you will need to utilize your
      //own server side storage.

      try
      {
         //lpRtHlt.setLatLngs(routeResults.getShapePoints());
         LatLngCollection collection = routeResults.getShapePoints();
         return collection;
        // lpRtHlt.getLatLngs().generalize(0.01);
      }
      catch (Exception e)
      {
    	  return null;
         //JOptionPane.showMessageDialog(this, " Exception: " + e,
         //"Exception", JOptionPane.ERROR_MESSAGE);
      }

      //Add the line primitive to a primitiveCollection
     // PrimitiveCollection pcMap = new PrimitiveCollection();
//      pcMap.add(lpRtHlt);
//      mqSession.addOne(pcMap);
//
//      //The best fit object is used to draw a map at an appropriate scale determined
//      //by the features you have added to the session, along with the optional primitives.
//      //In this case we want the map to be displayed at a scale that includes the origin
//      //and destination locations (pointFeatures) as well as the routeHighlight(linePrimitive).
//      //The scaleAdjustmentFactor is then used to increase the scale by this factor based
//      //upon the best fit that was performed.  This results in a border around the edge of
//      //the map that does not include any features so the map appears clearer.
//      BestFit bfMap = new BestFit();
//      bfMap.setScaleAdjustmentFactor(1.2);
//      bfMap.setIncludePrimitives(true);
//      mqSession.addOne(bfMap);
//
//      DisplayState displayState = new DisplayState();
//
//      //Generate the image src URL that will point back to the mapping server for the map.
//      try
//      {
//         mapURL = mapClient.getMapDirectURLEx(mqSession, displayState);
//
//		 // This call generates the actual GIF image resulting from the given Session Object.
//		 byte [] bImage = mapClient.getMapImageDirect(mqSession,displayState);
//
//		 FileOutputStream fileOutput = new FileOutputStream("mapImage.gif");
//		 fileOutput.write(bImage);
//		 fileOutput.close();	
//
//      }
//      catch (Exception e)
//      {
//         JOptionPane.showMessageDialog(this, " Exception: " + e,
//         "Exception", JOptionPane.ERROR_MESSAGE);
//      }
//
//      dataVector = new Vector();
//
//      columnNames = new Vector(4);
//      columnNames.add("MANEUVER");
//      columnNames.add("DIRECTIONS");
//      columnNames.add("DISTANCE(MILES)");
//      columnNames.add("TIME(MINUTES)");
//
//      df = new DecimalFormat("0.00");
//
//      try
//      {
//         //Each manuever object contains its own narrative, distance and drive time.
//         for (int mc = 0; mc < routeResults.getTrekRoutes().getAt(0).getManeuvers().getSize(); mc++)
//         {
//            Maneuver man = (Maneuver)routeResults.getTrekRoutes().getAt(0).getManeuvers().getAt(mc);
//            rowVector = new Vector(4);
//            rowVector.add(new Integer(mc+1));
//            rowVector.add(man.getNarrative());
//            rowVector.add(new Float(df.format(man.getDistance())));
//            rowVector.add(new Double((man.getTime() / 60.0)));
//            dataVector.add(rowVector);
//         }
//
//      }
//      catch (Exception e)
//      {
//         JOptionPane.showMessageDialog(this, " Exception: " + e,
//         "Exception", JOptionPane.ERROR_MESSAGE);
//      }
//
//      lcRouteLocations.removeAll();
//      routeResults.getShapePoints().removeAll();

   }//end doMapQuest
/*-------  End code to demonstrate use of the MapQuest server.  ------*/

}//end class DisplayFrameNoServer


//Creates a panel displaying the map image obtained from the URL.
class ImagePanelNoServer extends JPanel
{
   private Image image;
   private String mapURL;
   private int imageHeight;
   private int imageWidth;
   private MediaTracker tracker;
   private URL url;

   public ImagePanelNoServer(String mapURL, int imageWidth, int imageHeight)
   {
      this.mapURL = mapURL;
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;

      setSize(new Dimension(imageWidth, imageHeight));

      //Using the URL generated, create the map image.
      try
      {
         url = new URL(mapURL);
      }
      catch (MalformedURLException e)
      {
         JOptionPane.showMessageDialog(this, " MalformedURLException: " + e,
         "MalformedURLException", JOptionPane.ERROR_MESSAGE);
      }
      tracker = new MediaTracker(this);
      image = Toolkit.getDefaultToolkit().getImage(url);
      int id = 0;
      tracker.addImage(image, id);
      try
      {
         tracker.waitForID(id);
      }
      catch (InterruptedException e)
      {
         JOptionPane.showMessageDialog(this, " InterruptedException: " + e,
         "InterruptedException", JOptionPane.ERROR_MESSAGE);
      }

   }//end ImagePanelNoServer

   //The paintComponent is overridden to produce the image on the panel.
   public void paintComponent(Graphics g)
   {
      super.paintComponent(g);

      //Draw the map image
      g.drawImage(image, 120, 0, imageWidth, imageHeight, null);

   }//end paintComponent

}//end class ImagePanelNoServer
