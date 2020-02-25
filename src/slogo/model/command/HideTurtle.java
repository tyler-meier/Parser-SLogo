package slogo.model.command;

import slogo.model.Turtle;

public class HideTurtle extends Command {

    private static final double HIDDEN = 0;

    /**
     * Constructor for hide turtle which allows the turtle to be hidden by setting visibility to be false
     * Pass up the return value to the super constructor, which is 0
     * @param body the turtle that is currently being used, will set to be hidden
     */
    public HideTurtle(Turtle body){
        super(HIDDEN);
        body.setVisibility(HIDDEN);
    }
}
