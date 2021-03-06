package slogo.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import slogo.view.turtles.TurtleView;
import slogo.view.Visualizer;

public class XMLWriter {
  private Document myDocument;
  private Visualizer myVisualizer;

  public XMLWriter(Visualizer visualizer){
    myVisualizer = visualizer;
    setupDocument();
  }

  private void setupDocument() {
    DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = null;
    try {
      documentBuilder = documentFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      displayAndLogError("Failed to build document parser", e);
    }
    myDocument = documentBuilder.newDocument();
  }

  private void displayAndLogError(String header, Exception e) {
    Alert errorAlert = new Alert(AlertType.ERROR);
    errorAlert.setHeaderText(header);
    errorAlert.setContentText(e.getMessage());
    errorAlert.showAndWait();
  }

  /**
   * Saves an XML file at the given filepath
   * @param filepath where the user wants to save the XML created
   */
  public void saveXML(String filepath){
    createNodes();
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource domSource = new DOMSource(myDocument);
      StreamResult streamResult = new StreamResult(new File(filepath));
      transformer.transform(domSource, streamResult);
    }catch(TransformerException e){
      displayAndLogError("Failed to write game state into XML", e);
    }
  }

  private void createNodes(){
    Element root = myDocument.createElement("SLogo");
    myDocument.appendChild(root);
    root.appendChild(writePreferences());
    root.appendChild(writeTurtles());
    root.appendChild(writeCommandHistory());
    root.appendChild(writeUserVariables());
    root.appendChild(writeUserCommands());
    root.appendChild(writeColorPalette());
    root.appendChild(writePaths());
  }

  private Node writePreferences() {
    Element preferences = myDocument.createElement("Preferences");
    preferences.appendChild(createEndNode("Language", myVisualizer.getLanguage()));
    preferences.appendChild(createEndNode("Background", myVisualizer.getBackground().toString()));
    return preferences;
  }

  private Node writeTurtles(){
    Element turtles = myDocument.createElement("Turtles");
    String[] attributes = new String[]{"name", "xpos", "ypos", "heading"};
    for(String s : myVisualizer.getTurtles().keySet()){
      TurtleView currentTurtle = myVisualizer.getTurtles().get(s);
      Double[] attributeValues = currentTurtle.getData();
      String[] attributeStrings = new String[attributes.length];
      for(int i = 0; i < attributeValues.length; i++){
        attributeStrings[i+1] = attributeValues[i].toString();
      }
      attributeStrings[0] = myVisualizer.getTurtles().get(s).getName();
      turtles.appendChild(createAttributeNode("Turtle", attributes, attributeStrings));
    }
    return turtles;
  }

  private Node writeCommandHistory(){
    Element commands = myDocument.createElement("CommandHistory");
    List<String> cmdHistory = myVisualizer.getTerminal().getHistory();
    for(String s : cmdHistory){
      commands.appendChild(createAttributeNode("Command", new String[]{"syntax"}, new String[]{s}));
    }
    return commands;
  }

  private Node writeUserVariables(){
    Element variables = myDocument.createElement("UserVariables");
    Map<String, Double> userVariables = myVisualizer.getUserVariables();
    for(String s : userVariables.keySet()){
      variables.appendChild(createAttributeNode("Variable", new String[]{"name", "value"}, new String[]{s, userVariables.get(s).toString()}));
    }
    return variables;
  }

  private Node writeUserCommands(){
    Element commands = myDocument.createElement("UserCommands");
    Map<String, String> userCommands = myVisualizer.getUserCommands();
    for(String s : userCommands.keySet()){
      commands.appendChild(createAttributeNode("Command", new String[]{"name", "syntax"}, new String[]{s, userCommands.get(s)}));
    }
    return commands;
  }

  private Node createEndNode(String name, String value){
    Element node = myDocument.createElement(name);
    node.appendChild(myDocument.createTextNode(value));
    return node;
  }

  private Node writeColorPalette(){
    Element colors = myDocument.createElement("ColorPalette");
    Map<Double, String> colorMap = myVisualizer.getColorMap();
    for(Double d : colorMap.keySet()){
      colors.appendChild(createAttributeNode("Color", new String[]{"index", "color"}, new String[]{d.toString(), colorMap.get(d)}));
    }
    return colors;
  }

  private Node writePaths(){
    Element paths = myDocument.createElement("Paths");
    List<Path> pathList = new ArrayList<>(myVisualizer.getPaths());
    for(Path p : pathList){
      List<String> attributeValues = new ArrayList();
      for(PathElement pe: p.getElements()){
        attributeValues.addAll(Arrays.asList(pe.toString().split(" ")));
      }
      attributeValues.add(p.getStrokeWidth()+"");
      Color color = (Color) p.getStroke();
      String hex = String.format( "#%02X%02X%02X",
          (int)( color.getRed() * 255 ),
          (int)( color.getGreen() * 255 ),
          (int)( color.getBlue() * 255 ) );
      attributeValues.add(hex);
      attributeValues.add(p.getOpacity()+"");
      paths.appendChild(createAttributeNode("Path", new String[]{"x0", "y0", "x1", "y1", "stroke", "color", "opacity"}, attributeValues.toArray(new String[0])));
    }
    return paths;
  }

  private Node createAttributeNode(String name, String[] attributes, String[] attributeValues){
    Element node = myDocument.createElement(name);
    for(int i = 0; i < attributes.length; i++) {
      node.setAttribute(attributes[i], attributeValues[i]);
    }
    return node;
  }
}
