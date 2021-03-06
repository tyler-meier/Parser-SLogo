package slogo.view.turtles;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Transition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import slogo.view.paths.LinePathElement;
import slogo.view.paths.MoveToElement;

/**
 *
 */
public class TurtleAnimator {
  private static final double PATH_OPACITY = .75;
  private static final double PATH_NO_OPACITY = 0.0;

  private Queue<Path> pathHistory;
  private List<Path> backupPathHistory;
  private Queue<Transition> transitionQueue;
  private Queue<Transition> backupTransitionQueue;
  private TurtleView myTurtle;
  private SequentialTransition st;
  private Node myImage;
  private Group myPaths;
  private int animationDuration;
  private int totalDuration;
  private boolean stopped;

  public TurtleAnimator(TurtleView turtle, Node image, Group paths){
    myTurtle = turtle;
    pathHistory = new LinkedList<>();
    backupPathHistory = new LinkedList<>();
    transitionQueue = new LinkedList<>();
    backupTransitionQueue = new LinkedList<>();
    st = new SequentialTransition();
    totalDuration = 500;
    animationDuration = totalDuration;
    stopped = true;
    setShape(image);
    myPaths = paths;
  }
  /**
   * Updates the turtle's position, is called in the controller and updates the position whenever a corresponding command
   * is typed in.
   * @param newX - new x position
   * @param newY - new y position
   * @param orientation - new orientation
   */
  public void update(double newX, double newY, double orientation){
    if(transitionQueue.isEmpty()){
      myTurtle.updateHistory();
    }
    double[] newCoords = myTurtle.convertCoordinatesToView(newX, newY, orientation);
    double[] oldCoords = myTurtle.convertCoordinatesToView(myTurtle.getData()[0], myTurtle.getData()[1], myTurtle.getData()[2]);
    myTurtle.updateCurrent(newX, newY, orientation);

    if(newCoords[0] != oldCoords[0] || newCoords[1] != oldCoords[1]) {
      Path path = new Path();
      if(myTurtle.penDown()){
        path.setOpacity(PATH_OPACITY);
        path.setStrokeWidth(myTurtle.getLineWidth());
      } else {
        path.setOpacity(PATH_NO_OPACITY);
      }
      path.setStroke(myTurtle.getColor());
      path.getElements().add(new MoveToElement(oldCoords[0], oldCoords[1]));
      path.getElements().add(new LinePathElement(newCoords[0], newCoords[1]));
      PathTransition pt = new PathTransition(Duration.millis(animationDuration), path, myImage);
      pt.setPath(path);
      transitionQueue.add(pt);
      pathHistory.add(path);
    }

    if(newCoords[2] != oldCoords[2]) {
      RotateTransition rt = new RotateTransition(Duration.millis(animationDuration),
          myImage);
      rt.setFromAngle(oldCoords[2]);
      rt.setToAngle(newCoords[2]);
      transitionQueue.add(rt);
      pathHistory.add(new Path());
    }
  }

  /**
   * Once the turtle's position is updated, the animation is played in order to see the turtle move.
   */
  public void playAnimation(){
    backupTransitionQueue = new LinkedList<>(transitionQueue);
    backupPathHistory = new LinkedList<>(pathHistory);
    animateRecurse();
  }

  private void animateRecurse() {
    if(animationDuration == 0) {
      while(!transitionQueue.isEmpty()){
        st.getChildren().add(transitionQueue.remove());
        addPath();
        st.play();
      }
    } if(!transitionQueue.isEmpty()) {
      stopped = false;
      st = new SequentialTransition(transitionQueue.remove());
      st.setOnFinished(e -> {
        animateRecurse();
        addPath();
      });
      st.play();
    } else {
      stopped = true;
      st = new SequentialTransition();
      double[] modelCoords = myTurtle.convertCoordinatesToModel(myImage.getTranslateX()+myImage.getBoundsInLocal().getWidth()/2,
          myImage.getTranslateY()+myImage.getBoundsInLocal().getHeight()/2,
          myImage.getRotate());
      myTurtle.updateCurrent(modelCoords[0], modelCoords[1], modelCoords[2]);
    }
  }

  private void addPath() {
    Path path = pathHistory.remove();
    myPaths.getChildren().add(path);
    myTurtle.addPath(path);
  }

  public void pause(){
    st.pause();
  }

  public void play(){
    if(stopped){
      animateRecurse();
    } else {
      st.play();
    }
  }

  public void step(){
    if(!stopped || !transitionQueue.isEmpty()) {
      if(stopped) {
        st = new SequentialTransition(transitionQueue.remove());
      }
      st.setOnFinished(e-> {
        addPath();
        stopped = true;
      });
      st.play();
    }
  }

  public void resetAnimation(){
    stopped=true;
    st = new SequentialTransition();
    myTurtle.rewindAnimation();
    myPaths.getChildren().removeAll(backupPathHistory);
    transitionQueue = new LinkedList<>(backupTransitionQueue);
    pathHistory = new LinkedList<>(backupPathHistory);
  }

  public void undo(){
    stopped=true;
    st = new SequentialTransition();
    myTurtle.undoMove();
    transitionQueue = new LinkedList<>();
    myPaths.getChildren().removeAll(backupPathHistory);
    pathHistory = new LinkedList<>();
  }

  /**
   * Creates a designated size for the commands.
   * @param size
   */
  public void setCommandSize(int size){
    if(size == 0){
      return;
    }
    animationDuration = totalDuration / size;
    if(animationDuration == 0){
      animationDuration = 1;
    }
  }

  public void setSpeed(int value){
    totalDuration = value;
  }

  public void setShape(Node image){myImage = image;}
}
