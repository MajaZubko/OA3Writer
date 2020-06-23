package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {
    @FXML
    private Label comLabel;

    @FXML
    private TextField textField;

    @FXML
    private CheckBox differentPathCheckBox;

    @FXML
    private TextField differentPathField;

    @FXML
    private CheckBox dontDeleteCheckBox;

    @FXML
    private Button fileChooserButton;

    @FXML
    private Label differentPathLabel;

    @FXML
    private GridPane writerGridPane;

    @FXML
    private Button confirmationForward;


    private ArrayList<String> lines = new ArrayList<>();

    private ArrayList<String> memory = new ArrayList<>();

    private int trackingIndex = 0;
    private final String trackingInfo = "<TrackingInfo>";
    private int serialIndex = 0;
    private final String serialNumber = "<SerialNumber";

    private Path filePath = Paths.get("C:\\OA\\OA3.cfg");

    private String potentialFilePathString = "";

    private boolean done = false;

    private boolean delete = true;

    private boolean differentPathSelected = false;

    private boolean validPath = true;

    private String pathFromMemory = "";

    private String currentDirectory = "";


    public void initialize() {
        //get current path
        currentDirectory = System.getProperty("user.dir");

        //load memory
        File memoryFile = new File(currentDirectory + "\\memory.txt");
        try (BufferedReader br = Files.newBufferedReader(Paths.get(memoryFile.getPath()))) {
            String line;
            while ((line = br.readLine()) != null) {
//                System.out.println(line);
                memory.add(line);
            }
            if (memory.size() == 3) {
                delete = Boolean.parseBoolean(memory.get(0));
                differentPathSelected = Boolean.parseBoolean(memory.get(1));
                pathFromMemory = memory.get(2);

//                System.out.println(pathFromMemory);
            }
            dontDeleteCheckBox.setSelected(!delete);
            differentPathCheckBox.setSelected(differentPathSelected);
            handlePathChange();
            if ((!pathFromMemory.isEmpty())) {
                filePath = Paths.get(pathFromMemory);
                differentPathField.setText(pathFromMemory);
            }

        } catch (IOException e) {
            System.out.println("Problems reading the memory: " + e.getMessage());
            if (!memoryFile.exists()) {
                comLabel.setText("No memory");
            }
            comLabel.setText("Problems reading the memory");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleKeyPressed(KeyEvent keyEvent) {
        String value = textField.getText();
        if (!value.isEmpty()) {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                read();
                String desirable = "\\d+";
                Pattern desirablePattern = Pattern.compile(desirable);
                Matcher desirableMatcher = desirablePattern.matcher(value);
                if (!desirableMatcher.matches()) {
                    comLabel.setText("Invalid input");
                } else {
                    value = value.trim();
                    String trackingInfoGroup = "(<TrackingInfo>)(.*?)(</TrackingInfo>)";
                    Pattern trackingGroupPattern = Pattern.compile(trackingInfoGroup);
                    Matcher trackingGroupMatcher = trackingGroupPattern.matcher(lines.get(trackingIndex));
                    String trackingLine = "";
                    String trackingLineToReplace = "";
                    while (trackingGroupMatcher.find()) {
                        trackingLine = trackingGroupMatcher.group();
                        trackingLineToReplace = trackingGroupMatcher.group(2);
                    }
                    if (trackingLine != "" && trackingLineToReplace != "") {
                        String newTrackingLine = trackingLine.replace(trackingLineToReplace, value);
                        lines.set(trackingIndex, newTrackingLine);
                    }

                    String serialNumberGroup = "(<SerialNumber>)(.*?)(</SerialNumber>)";
                    Pattern serialGroupPattern = Pattern.compile(serialNumberGroup);
                    Matcher serialGroupMatcher = serialGroupPattern.matcher(lines.get(serialIndex));
                    String serialLine = lines.get(serialIndex);
                    String serialLineToReplace = "";
                    while (serialGroupMatcher.find()) {
                        serialLineToReplace = serialGroupMatcher.group(2);
                    }
                    if (serialLine != "" && serialLineToReplace != "") {
                        String newSerialLine = serialLine.replace(serialLineToReplace, value);
                        lines.set(serialIndex, newSerialLine);
                    }
                    //write new file
                    try (BufferedWriter newFile = Files.newBufferedWriter(filePath)) {
                        for (int i = 0; i < lines.size(); i++) {
                            newFile.write(lines.get(i) + "\n");
                        }

                        done = true;
                        comLabel.setText("Correctly replaced");
                    } catch (IOException e) {
                        System.out.println("Problems while writing the file: " + e.getMessage());
                        comLabel.setText("Problems while writing the file");
                    }


                    if (done) {

                        //write memory
                        writeMemory();
                        //close the program

                        handleDeleteChange();

                        if (!delete) {
                            justExit();
                        } else {
                            exitAndDelete();
                        }
                    }
                }
            }
        }
    }


    @FXML
    public void handleDeleteChange() {
        delete = !dontDeleteCheckBox.isSelected();
    }

    @FXML
    public void handlePathChange() {
        if (differentPathCheckBox.isSelected()) {
            differentPathField.setVisible(true);
            fileChooserButton.setVisible(true);
            confirmationForward.setVisible(true);
            textField.setDisable(true);
        } else {
            differentPathField.setVisible(false);
            fileChooserButton.setVisible(false);
            confirmationForward.setVisible(false);
            textField.setDisable(false);
            filePath = Paths.get("C:\\OA\\OA3.cfg");
        }
    }

    @FXML
    public void handleChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose OA3 file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OA3 File", "*.cfg"));

        List<File> file = chooser.showOpenMultipleDialog(writerGridPane.getScene().getWindow());
        if (file != null) {
            potentialFilePathString = file.get(0).getPath();
            differentPathField.setText(potentialFilePathString);
            File newFile = new File(differentPathField.getText());
            while (!newFile.exists()) {
//                differentPathLabel.setText("Invalid path - file does not exist");
                validPath = false;
            }
//            differentPathField.setText(potentialFilePathString);
            if (validPath) {
                filePath = Paths.get(differentPathField.getText());
            }
//            System.out.println(filePath.toAbsolutePath());
            confirmationForward.setDisable(false);
//            textField.setDisable(false);
        }
    }

    @FXML
    public void handleConfirmationNewPath() {
        File newFile = new File(differentPathField.getText());
        if (!newFile.exists()) {
            differentPathLabel.setText("Invalid path - file does not exist");
            validPath = false;
        }

        if (validPath) {
            textField.setDisable(false);
            filePath = Paths.get(differentPathField.getText());
        }
    }

    public void read() {
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
//                System.out.println(line);
            }
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(trackingInfo)) {
                    trackingIndex = i;
                }
            }
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(serialNumber)) {
                    serialIndex = i;
                }
            }
        } catch (IOException e) {
            System.out.println("Problems reading the file: " + e.getMessage());
            comLabel.setText("Problems reading the file");
            e.printStackTrace();
        }
    }

    public void writeMemory() {
        try (BufferedWriter memoryFile = Files.newBufferedWriter
                (Paths.get(currentDirectory + "\\memory.txt"))) {
            memoryFile.write(delete + "\n");
            memoryFile.write(differentPathCheckBox.isSelected() + "\n");
            memoryFile.write(filePath.toAbsolutePath() + "\n");
        } catch (IOException e) {
            System.out.println("Problems while writing the memory: " + e.getMessage());
            comLabel.setText("Problems while writing the memory");
        }
    }

    public void exitAndDelete(){
        try {
            Runtime.getRuntime().exec("cmd /c ping localhost -n 6 > nul && del OA3Writer.jar ");
            Platform.exit();
        } catch (IOException e) {
            comLabel.setText("Problems while closing the program");
        }
    }

    public void justExit(){
        Platform.exit();
    }
}
