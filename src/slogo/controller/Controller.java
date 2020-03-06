package slogo.controller;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import slogo.exceptions.*;
import slogo.model.CommandList;
import slogo.model.Parser;
import slogo.model.Turtle;
import slogo.model.command.*;
import slogo.view.ViewExternal;
import slogo.fun.RomanNumerals;

public class Controller {
    private static final String LANGUAGES_PACKAGE = "resources.languages.";
    private static final String ERROR_PACKAGE = "resources.information.ErrorText";
    private static final String INFORMATION_PACKAGE = "resources.information.";
    private static final String COMMAND_PACKAGE = "slogo.model.command.";
    private static final String COMMAND = "Command";
    private static final String CONSTANT = "Constant";
    private static final String NO_MATCH = "NO MATCH";
    private static final String VARIABLE = "Variable";
    private static final String MAKE_VARIABLE = "MakeVariable";
    private static final String WHITESPACE = "\\s+";
    private static final int ZERO = 0;
    private static final int ONE_DOUBLE_PARAM_VALUE = 1;
    private static final int TWO_DOUBLE_PARAM_VALUE = 2;
    private static final Integer SECOND_GEN = 2;
    private static final double ZERO_DOUBLE_PARAM_VALUE = 0;
    private static final double TURTLE_PARAM_VALUE = -0.5;
    private static boolean IS_VARIABLE = false;

    private List<Entry<String, Pattern>> mySymbols;
    private CommandList commandCreator;
    private Stack<String> commandStack;
    private Stack<Double> argumentStack;
    private Stack<Integer> doubleParametersStack, listParametersStack;
    private Stack<List<Command>> listStack;
    private Stack<Stack> commandStackHolder, argumentStackHolder, parametersStackHolder, listParametersStackHolder, listStackHolder;
    private Stack<List<Command>> currentListHolder = new Stack<>();
    private Map<String, List> userCreatedCommandVariables;
    private Map<String, String> userCreatedConstantVariables;
    private Map<String, Turtle> nameToTurtle;
    private Map<Turtle, Double> turtleId;
    private Map<String, Integer> nameCount;
    private Turtle turtle;
    private String myCommands;
    private ViewExternal myView;
    private double IdOfTurtle;
    private Parser commandParser, parametersParser, syntaxParser, listParamsParser, doubleParamsParser;
    private List<Command> currentList, extendedList;
    private ResourceBundle errorResources;

    /**
     * The constructor for controller class, initializes the view, list of
     * symbols that is being used for parsing, map of created variables,
     * all of the stacks used, and the parsers for each different stack
     * @param visualizer the view of the program
     * @param language the specific language being used (aka english, chinese, etc)
     */
    public Controller(ViewExternal visualizer, String language) {
        commandCreator = new CommandList(this, language);
        errorResources = ResourceBundle.getBundle(ERROR_PACKAGE);
        myView = visualizer;
        mySymbols = new ArrayList<>();

        makeMaps();
//        makeStacks();
    }

//    private void makeParsers(String language){
//        addLanguage(language);
//
//        syntaxParser = new Parser(LANGUAGES_PACKAGE);
//        syntaxParser.addPatterns("Syntax");
//
//        parametersParser = new Parser(INFORMATION_PACKAGE);
//        parametersParser.addPatterns("Parameters");
//
//        listParamsParser = new Parser(INFORMATION_PACKAGE);
//        listParamsParser.addPatterns("ListParameters");
//
//        doubleParamsParser = new Parser(INFORMATION_PACKAGE);
//        doubleParamsParser.addPatterns("TurtleAndDoubleParameters");
//    }

    private void makeMaps() {
        userCreatedCommandVariables = new HashMap<>();
        userCreatedConstantVariables = new HashMap<>();
        nameToTurtle = new HashMap<>();
        nameCount = new HashMap<>();
        turtleId = new HashMap<>();
    }

//    private void makeStacks() {
//        makeNewStacks();
//        listParametersStack = new Stack<>(); //todo figure out why we separated this one out
//    }
//
//    private void makeNewStacks(){ //INTENTIONALLY MAKING NEW STACKS RATHER THAN CLEARING
//        commandStack = new Stack<>();
//        argumentStack = new Stack<>();
//        doubleParametersStack = new Stack<>();
//        listStack = new Stack<>();
//    }

//    private void makeHolderStacks() {
//        commandStackHolder = new Stack<>();
//        argumentStackHolder = new Stack<>();
//        parametersStackHolder = new Stack<>();
//        listParametersStackHolder = new Stack<>();
//        listStackHolder = new Stack<>();
//        currentListHolder = new Stack<>();
//    }
//
//    private void holdStacks(){
//        commandStackHolder.push(commandStack);
//        argumentStackHolder.push(argumentStack);
//        parametersStackHolder.push(doubleParametersStack);
//        listParametersStackHolder.push(listParametersStack);
//        listStackHolder.push(listStack);
//        makeNewStacks();
//        listStack =  listStackHolder.pop();
//        currentListHolder.push(currentList);
//    }
//
//    private void stopHoldingStacks(){
//        commandStack = commandStackHolder.pop();
//        argumentStack = argumentStackHolder.pop();
//        doubleParametersStack = parametersStackHolder.pop();
//        listParametersStack = listParametersStackHolder.pop();
//        currentList = currentListHolder.pop();
//    }

    /**
     * Add a new turtle to the map of turtles, change the current turtle to
     * this newly created turtle; turtle with a default "name" that is its hashcode
     */
    public void addTurtle(){
        IdOfTurtle++;
        turtle = new Turtle(IdOfTurtle);
        if(nameCount.containsKey(turtle.getName())){
            Integer generation = nameCount.get(turtle.getName());
            nameCount.put(turtle.getName(), nameCount.get(turtle.getName())+1);
            RomanNumerals rn = new RomanNumerals();
            turtle.setName(turtle.getName() + " " + rn.intToNumeral(generation));
        }
        nameCount.putIfAbsent(turtle.getName(), SECOND_GEN);
        nameToTurtle.putIfAbsent(turtle.getName(), turtle);
        commandCreator.setTurtle(turtle);
    }

    /**
     * Adds a new turtle to the screen with the given parameters
     * @param name new name of the turtle
     * @param startingX the x position of where the turtle will start
     * @param startingY the y position of where the turtle will start
     * @param startingHeading where the turtle will be facing
     */
    public void addTurtle(String name, double startingX, double startingY, double startingHeading){
        IdOfTurtle++;
        Turtle t = new Turtle(name, startingX, startingY, startingHeading, IdOfTurtle);
        if(nameToTurtle.containsKey(t.getName())){
            throw new InvalidTurtleException("Turtle already exists", new Throwable()); //shouldn't ever get to this
        }
        nameToTurtle.putIfAbsent(t.getName(), t);
        turtle = t;
        commandCreator.setTurtle(t);
    }

    /**
     * get the name of the current turtle
     * @return turtle's name
     */
    public String getTurtleName(){
        return turtle.getName();
    }

    /**
     * Allows a command variable to be updated in the UI
     * @param key the old command value
     * @param newValue what it will be changed to
     */
    public void updateCommandVariable(String key, String newValue){
        if(userCreatedConstantVariables.containsKey(key)){
            userCreatedConstantVariables.put(key, newValue);
        }
    }

    /**
     * Add a user created variable to the map of user created variables
     * @param key variable name
     * @param value variable value
     */
    public void addUserVariable(String key, String value){
        userCreatedConstantVariables.putIfAbsent(key, value);
    }

    /**
     * add a user created command to the map of user created commands
     * @param key variable name
     * @param syntax variable commands
     */
    public void addUserCommand(String key, String syntax){
        List<String> commandList = Arrays.asList(syntax.split(" "));
        userCreatedCommandVariables.putIfAbsent(key, commandList);
    }

    /**
     * Allows the user to pick a turtle to do work on
     * @param name turtle to become the current turtle
     */
    public void chooseTurtle(String name){
        turtle = nameToTurtle.get(name);
        commandCreator.setTurtle(turtle);
    }

    /**
     * Change to a new language of input
     * @param language input language: English, Spanish, Urdu, etc.
     */
    public void addLanguage(String language){
        commandCreator.setLanguage(language);
    }

    /**
     * Receives the commands to be done from the view/UI
     * @param commands the commands the user typed in
     */
    public void sendCommands(String commands){
//        List<String> lines = asList(commands.split("\\n"));
//        List<Command> commandList = new ArrayList<>();
//        for(String l : lines){
//            commandList.addAll(parseText(syntaxParser, commandParser, parametersParser, asList(commands.split(WHITESPACE))));
//        }
        executeCommandList(commandCreator.getCommandsOf(commands));
    }

//    private List<String> asList(String[] array){
//        List<String> list = new ArrayList<>();
//        for(String s : array){ list.add(s); }
//        return list;
//    }
//
//    private List<Command> parseText (Parser syntax, Parser lang, Parser params, List<String> lines) {
//        currentList = new ArrayList<>();
//        ListIterator<String> iterator = lines.listIterator();
//        String isFirstConstant = lines.get(ZERO);
//
//        if (syntax.getSymbol(isFirstConstant).equals(CONSTANT)){
//            throw new InvalidConstantException(new Throwable(), errorResources.getString("StartWithConstant"));
//        }
//        makeHolderStacks();
//        while(iterator.hasNext() && !IS_VARIABLE) {
//            String line = iterator.next();
//            if (line.trim().length() > 0) {
//                String commandSyntax = syntax.getSymbol(line); //get what sort of thing it is
//                if(commandSyntax.equals(COMMAND)){
//                    doCommandWork(params, lang, syntax, currentList, line, commandSyntax, lines);
//                } else if (commandSyntax.equals(CONSTANT)){
//                    doConstantWork(line, currentList);
//                } else if (commandSyntax.equals(VARIABLE)){
//                    if(userCreatedCommandVariables.containsKey(line)){
//                        doCommandVariable(line, syntax, lang, params, currentList);
//                    } else if (userCreatedConstantVariables.containsKey(line)){
//                        doConstantVariable(line, currentList);
//                    } else {
//                        throw new InvalidVariableException(new Throwable(), errorResources.getString("FakeVariable"));
//                    }
//                } else if (commandSyntax.equals("ListStart")){
//                    doListStartWork();
//                } else if (commandSyntax.equals("ListEnd")){
//                    doListEndWork();
//                }
//            }
//        }
//        IS_VARIABLE = false;
//        while(!commandStack.isEmpty()){
//            tryToMakeCommands(currentList);
//        }
//        //executeCommandList(createExtendedList(currentList));
//        return createExtendedList(currentList);
//    }
//
//    private void doListStartWork(){
//        holdStacks();
//        currentList = new ArrayList<>();
//    }
//
//    private void doListEndWork(){
//        if(!currentList.isEmpty()) {
//            listStack.push(currentList);
//        }
//        stopHoldingStacks();
//        tryToMakeCommands(currentList);
//    }

//    private void doCommandWork(Parser params, Parser lang, Parser syntax, List<Command> commandList, String line, String commandSyntax, List<String> lines){
//        String commandName = lang.getSymbol(line); //get the string name, such as "Forward" or "And"
//        if (commandName.equals(NO_MATCH)){
//            throw new InvalidCommandException(new Throwable(), commandSyntax, line);
//        }
//        else if (commandName.equals(MAKE_VARIABLE)){
//            dealWithMakingVariables(lines, line, syntax);
//        } else {
//            validCommand(params, commandName, commandList);
//        }
//    }

    private void dealWithMakingVariables(List<String> lines, String line, Parser syntax){
        IS_VARIABLE = true;
        List<String> copyLines = new CopyOnWriteArrayList(lines);
        copyLines.remove(line);
        String variable = copyLines.get(0);
        String firstCommand = copyLines.get(1);
        String type = syntax.getSymbol(firstCommand);
        copyLines.remove(variable);
        if (copyLines.size() > 1 || (copyLines.size() == 1 && type.equals("Command"))){
            if (!userCreatedConstantVariables.containsKey(variable)){
                userCreatedCommandVariables.put(variable, copyLines);
                String commandSyntax = String.join(" ", copyLines.toArray(new String[0]));
                myView.addCommand(variable, commandSyntax);
            } else {
                throw new InvalidVariableException(new Throwable(), errorResources.getString("AlreadyConstant"));
            }
        } else if (copyLines.size() == 1 && type.equals("Constant")){
            if (!userCreatedCommandVariables.containsKey(variable)){
                userCreatedConstantVariables.put(variable, firstCommand);
                myView.addVariable(variable, firstCommand);
            } else {
                throw new InvalidVariableException(new Throwable(), errorResources.getString("AlreadyCommand"));
            }
        }
    }

    //todo finish comment
    /**
     * get the user created constant or variable in a line from the map
     * @param line
     * @return
     */
    public String getUserCreatedConstantVariables(String line){
        return userCreatedConstantVariables.get(line);
    }

    //todo finish comment
    /**
     *
     * @param line
     * @return
     */
    public List<String> getUserCreatedCommandVariables(String line){
        return userCreatedCommandVariables.get(line);
    }

    //todo finish comment
    /**
     *
     * @param line
     * @return
     */
    public boolean hasUserCreatedCommandVariables(String line){
        return userCreatedCommandVariables.containsKey(line);
    }

    //todo finish comment
    /**
     *
     * @param line
     * @return
     */
    public boolean hasUserCreatedConstantVariables(String line){
        return userCreatedConstantVariables.containsKey(line);
    }

    //todo finish comment
    /**
     *
     * @param variable
     * @param copyLines
     */
    public void addUserCreatedCommand(String variable, List<String> copyLines){
        if (!userCreatedConstantVariables.containsKey(variable)){
            userCreatedCommandVariables.put(variable, copyLines);
            String commandSyntax = String.join(" ", copyLines.toArray(new String[ZERO]));
            myView.addCommand(variable, commandSyntax);
        } else {
            throw new InvalidVariableException(new Throwable(), errorResources.getString("AlreadyConstant"));
        }
    }

    public void addUserCreatedVariable(String variable, String firstCommand){
        if (!userCreatedCommandVariables.containsKey(variable)){
            userCreatedConstantVariables.put(variable, firstCommand);
            myView.addVariable(variable, firstCommand);
        } else {
            throw new InvalidVariableException(new Throwable(), errorResources.getString("AlreadyCommand"));
        }
    }

//    private List<Command> validCommand(Parser params, String commandName, List<Command> commandList) {
//        commandStack.push(commandName); //add string to stack
//        pushParamsNeeded(params.getSymbol(commandName)); //get Parameters string, and convert that string to a double
//        return tryToMakeCommands(commandList);
//    }
//
//    private void doCommandVariable(String line, Parser syntax, Parser lang, Parser params, List<Command> commandList){
//        List<String> variableDoesWhat = userCreatedCommandVariables.get(line);
//
//        for (String s : variableDoesWhat){
//            String comSyntax = syntax.getSymbol(s);
//            if (comSyntax.equals(COMMAND)){
//                doCommandWork(params, lang, syntax, commandList, s, comSyntax, variableDoesWhat);
//            } else if (comSyntax.equals(CONSTANT)){
//                doConstantWork(s, commandList);
//            }
//        }
//    }
//
//    private void doConstantVariable(String line, List commandList){
//        doConstantWork(userCreatedConstantVariables.get(line), commandList);
//    }
//
//    private void doConstantWork(String constant, List<Command> commandList){
//        argumentStack.push(Double.parseDouble(constant));
//        tryToMakeCommands(commandList);
//    }
//
//    private void pushParamsNeeded(String commandParams){
//        String listParamString = listParamsParser.getSymbol(commandParams);
//        if(listParamString.equals(NO_MATCH)){
//            throw new InvalidPropertyException(new Throwable(), errorResources.getString("InvalidPropertiesFile"));
//        } else {
//            listParametersStack.push(Integer.parseInt(listParamString));
//        }
//
//        String doubleParamsString = doubleParamsParser.getSymbol(commandParams);
//        if(doubleParamsString.equals(NO_MATCH)){
//            throw new InvalidPropertyException(new Throwable(), errorResources.getString("InvalidPropertiesFile"));
//        } else {
//            doubleParametersStack.push(Integer.parseInt(doubleParamsString)); //add that value to the params stack
//        }
//    }
//
//    private List<Command> tryToMakeCommands(List<Command> commandList){
//        if(checkArgumentStack() && checkListStack()){
//            commandList.add(weHaveEnoughArgumentsToMakeACommand(commandList));
//        }
//        return commandList;
//    }
//
//    private boolean checkArgumentStack(){
//        return !doubleParametersStack.isEmpty() && argumentStack.size() >= doubleParametersStack.peek();
//    }
//
//    private boolean checkListStack(){
//        return !listParametersStack.isEmpty() && (listStack.size() >= listParametersStack.peek());
//    }
//
//    private Command weHaveEnoughArgumentsToMakeACommand(List<Command> commands){
//        Command newCommand = getCommand(commandStack.pop());
//        if(!commandStack.isEmpty()){
//            argumentStack.push(newCommand.getResult());
//            tryToMakeCommands(commands); //slightly recursive :D
//        }
//        return newCommand;
//    }
//
//    private Command getCommand(String commandName){
//        try{
//            Class commandClass = Class.forName(COMMAND_PACKAGE + commandName);
//            return makeCommand(commandConstructor(commandClass));
//        } catch (ClassNotFoundException e){
//            throw new NoClassException(new Throwable(), errorResources.getString("NoClass"));
//        } catch (NoSuchMethodException e){
//            throw new NoClassException(new Throwable(), errorResources.getString("NoMethod"));
//        } catch (IllegalAccessException e) {
//            throw new IllegalException(new Throwable(), errorResources.getString("Illegal"));
//        } catch (InstantiationException e) {
//            throw new InstantException(new Throwable(), errorResources.getString("Instantiation"));
//        } catch (InvocationTargetException e) {
//            throw new InvocationException(new Throwable(), errorResources.getString("Invocation"));
//        }
//    }
//
//    private Constructor commandConstructor(Class command) throws NoSuchMethodException {
//        return command.getConstructor(List.class, List.class, List.class);
//    }
//
//    private Command makeCommand(Constructor constructor) throws IllegalAccessException, InvocationTargetException, InstantiationException {
//        List<Turtle> turtleListToGive = new ArrayList<>();
//        turtleListToGive.add(turtle/*activeTurtles*/); //fixme
//
//        return (Command) constructor.newInstance(turtleListToGive, doubleListToGive(), commandListToGive());
//    }
//
//    private List<Double> doubleListToGive(){
//        List<Double> doubles = new ArrayList<>();
//        if(doubleParametersStack.isEmpty()){
//            return doubles;
//        }
//        int numberOfDoublesNeeded = doubleParametersStack.pop();
//        for(int k=0; k < numberOfDoublesNeeded; k++){
//            doubles.add(argumentStack.pop());
//        }
//        return doubles;
//    }
//
//    private List<List<Command>> commandListToGive(){
//        List<List<Command>> lists = new ArrayList<>();
//        if(listParametersStack.isEmpty()){
//            return lists;
//        }
//        int numberOfListsNeeded = listParametersStack.pop();
//        for(int k=0; k < numberOfListsNeeded; k++){
//            lists.add(listStack.pop());
//        }
//        return lists;
//    }
//
//    private List<Command> createExtendedList(List<Command> l){
//        extendedList = new ArrayList<>();
//        for(Command c : l){
//            extendedList.addAll(c.getCommandList());
//        }
//        myView.setCommandSize(extendedList.size());
//        return extendedList;
//    }

    private void executeCommandList(List<Command> l){
        for(Command c : l){
            //System.out.println(c);
            /*System.out.println(*/c.execute()/*)*/;
            //System.out.println("DURING:: x: " + turtle.getX() + " y: " + turtle.getY() + " heading: " + turtle.getHeading());
            if(c instanceof ClearScreen){
                myView.updatePenStatus(0);
                myView.update(turtle.getX(),turtle.getY(), turtle.getHeading());
                myView.clear();
                myView.updatePenStatus(1);
            } else if(c instanceof HideTurtle || c instanceof ShowTurtle){ //FIXME get rid of instance of. maybe commands can have a string that returns a view method to call and then we use reflection for making the method with one parameter (getResult())...
                myView.updateTurtleView(c.getResult());
            } else if(c instanceof PenDown || c instanceof PenUp){
                myView.updatePenStatus(c.getResult());
            } else if(c instanceof SetBackground){
                myView.updateBackgroundColor(c.getResult());
            } else if(c instanceof SetPenColor){
                myView.updateCommandPenColor(c.getResult());
            } else if(c instanceof SetShape){
                System.out.println(c.getResult());
                myView.updateShape(c.getResult());
            } else if(c instanceof SetPenSize){
                myView.updatePenSize(c.getResult());
            } else if(c instanceof ID){
                System.out.println(c.getResult());
            }
            else {
                myView.update(turtle.getX(), turtle.getY(), turtle.getHeading());
            }
        }
//        makeNewStacks();
        myView.updateStatus();
    }
}