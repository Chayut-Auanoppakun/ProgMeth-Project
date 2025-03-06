package gui;

import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

/**
 * Simple Circle class for the MeetingUI
 */
public class Circle extends Shape {
    private double radius;
    private Color fill;
    
    public Circle(double radius, Color fill) {
        this.radius = radius;
        this.fill = fill;
    }
}