package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.utils.MathUtilities;
import org.firstinspires.ftc.teamcode.utils.Constants;

//TODO: re-tune odometry
//TODO: re-tune drive motors
//TODO: fix spline, rework undefined cases

/**
 * The robot drivetrain
 */
public class Drivetrain implements Constants {
    public DcMotorEx frontLeft, frontRight, backLeft, backRight, leftDead, rightDead, centerDead;
    private int previousLeft, previousRight, previousCenter;
    private double x, y, heading, desiredHeading;
    private boolean alliance;

    /**
     * Initializes the Drivetrain object
     *
     * @param hwMap the hardwareMap
     * @param alliance true for blue, false for red
     * @param x the starting x coordinate
     * @param y the starting y coordinate
     */
    public void init(HardwareMap hwMap, boolean alliance, double x, double y, double heading)
    {
        // Initialize Variables
        this.alliance = alliance;
        this.previousLeft = 0;
        this.previousRight = 0;
        this.previousCenter = 0;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.desiredHeading = heading;

        // Initialize Motors
        backLeft = hwMap.get(DcMotorEx.class, "backLeftDrive");
        backRight = hwMap.get(DcMotorEx.class, "backRightDrive");
        frontLeft = hwMap.get(DcMotorEx.class, "frontLeftDrive");
        frontRight = hwMap.get(DcMotorEx.class, "frontRightDrive");
        leftDead = hwMap.get(DcMotorEx.class, "leftDead");
        rightDead = hwMap.get(DcMotorEx.class, "rightDead");
        centerDead = hwMap.get(DcMotorEx.class, "centerDead");

        // Set Motor Directions
        backLeft.setDirection(DcMotorEx.Direction.REVERSE);
        backRight.setDirection(DcMotorEx.Direction.FORWARD);
        frontLeft.setDirection(DcMotorEx.Direction.REVERSE);
        frontRight.setDirection(DcMotorEx.Direction.FORWARD);
        leftDead.setDirection(DcMotorEx.Direction.FORWARD);
        rightDead.setDirection(DcMotorEx.Direction.REVERSE);
        centerDead.setDirection(DcMotorEx.Direction.FORWARD);

        // Set Zero Power Behavior
        backLeft.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        frontLeft.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
//        backLeft.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
//        backRight.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
//        frontLeft.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
//        frontRight.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.FLOAT);
        leftDead.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        rightDead.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        centerDead.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);

        // Stop and Reset Encoders
        backLeft.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        backRight.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        frontLeft.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        frontRight.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        leftDead.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        rightDead.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        centerDead.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);

        // Set Motor RunModes
        backLeft.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        backRight.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        frontLeft.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        frontRight.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        leftDead.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
        rightDead.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
        centerDead.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);

        // Set Motor PIDF Coefficients
        backLeft.setVelocityPIDFCoefficients(BACK_LEFT_P, BACK_LEFT_I, BACK_LEFT_D, BACK_LEFT_F);
        backRight.setVelocityPIDFCoefficients(BACK_RIGHT_P, BACK_RIGHT_I, BACK_RIGHT_D, BACK_RIGHT_F);
        frontLeft.setVelocityPIDFCoefficients(FRONT_LEFT_P, FRONT_LEFT_I, FRONT_LEFT_D, FRONT_LEFT_F);
        frontRight.setVelocityPIDFCoefficients(FRONT_RIGHT_P, FRONT_RIGHT_I, FRONT_RIGHT_D, FRONT_RIGHT_F);
    }

    /**
     * A testing and tuning method, spins motors at equal power
     *
     * @param power the power proportion to spin motors  [-1, 1]
     */
    public void driveTest(double power)
    {
        power = Range.clip( power, -1.0, 1.0);
        backLeft.setVelocity(power * MAX_SPIN_SPEED);
        backRight.setVelocity(power * MAX_SPIN_SPEED);
        frontLeft.setVelocity(power * MAX_SPIN_SPEED);
        frontRight.setVelocity(power * MAX_SPIN_SPEED);
    }

    /**
     * Drives the robot field-oriented
     *
     * @param power the driving power
     * @param angle the angle to drive at in degrees
     * @param turn the turning power
     * @param autoAlign whether to autoAlign
     * @param fieldOriented whether to drive field oriented
     */
    public void drive(double power, double angle, double turn, boolean autoAlign, boolean fieldOriented)
    {
        if(autoAlign && fieldOriented)
            turn = turnToAngle();
        else
            turn = Range.clip(turn, -MANUAL_TURNING_GOVERNOR, MANUAL_TURNING_GOVERNOR);

        power = Range.clip(power, turn - OVERALL_GOVERNOR, OVERALL_GOVERNOR - turn);

        double corner1;
        double corner2;

        if(fieldOriented) {
            corner1 = power * Math.sin(Math.toRadians(MathUtilities.addAngles(angle, -45.0 + 90.0 - heading)));
            corner2 = power * Math.sin(Math.toRadians(MathUtilities.addAngles(angle, 45.0 + 90.0 - heading)));
        }
        else {
            corner1 = power * Math.sin(Math.toRadians(MathUtilities.addAngles(angle, -45.0)));
            corner2 = power * Math.sin(Math.toRadians(MathUtilities.addAngles(angle, 45.0)));
        }

        backLeft.setVelocity((corner1 + turn) * MAX_SPIN_SPEED);
        backRight.setVelocity((corner2 - turn) * MAX_SPIN_SPEED);
        frontLeft.setVelocity((corner2 + turn) * MAX_SPIN_SPEED);
        frontRight.setVelocity((corner1 - turn) * MAX_SPIN_SPEED);
    }

    /**
     * Spins the robot anchor-less to a given heading smoothly using PID
     *
     * @return the turning speed as a proportion
     */
    public double turnToAngle() {
        double error = MathUtilities.addAngles(heading, -desiredHeading);
        if(Math.abs(error) >= TURNING_ERROR)
            return Range.clip(error * TURNING_P, -AUTO_TURNING_GOVERNOR, AUTO_TURNING_GOVERNOR);
        return 0.0;
    }

    /**
     * Sets the heading to auto-align to
     *
     * @param desiredHeading the heading in degrees [-180, 180)
     */
    public void setDesiredHeading(double desiredHeading) {
        this.desiredHeading = desiredHeading;
    }

    /**
     * Automatically directs the robot to the Coordinates of the Correct Intake
     * using parabolas in piecewise.
     */
    public void splineToIntake(double turn, boolean autoAlign) {
        double power = Range.clip(SPLINE_P * Math.sqrt(
                Math.pow(INTAKE_X - x, 2) + Math.pow(alliance ? BLUE_INTAKE_Y - y : RED_INTAKE_Y - y, 2)), -1.0, 1.0);

        double angle;
        if(x <= LEFT_WAYPOINT_X)
            angle = angleToVertex(LEFT_WAYPOINT_X, alliance ? BLUE_WAYPOINT_Y : RED_WAYPOINT_Y, true);
        else if(x <= RIGHT_WAYPOINT_X)
            angle = angleToVertex(RIGHT_WAYPOINT_X, alliance ? BLUE_WAYPOINT_Y : RED_WAYPOINT_Y, true);
        else
            angle = angleToVertex(RIGHT_WAYPOINT_X, alliance ? BLUE_WAYPOINT_Y : RED_WAYPOINT_Y, true);

        drive(power, angle, turn, autoAlign, true);
    }

    /**
     * Automatically directs the robot to the Coordinates of the Correct Backstage
     * using parabolas in piecewise.
     */
    public void splineToScoring(double turn, boolean autoAlign) {
        double power = Range.clip(SPLINE_P * Math.sqrt(
                Math.pow(SCORING_X - x, 2) + Math.pow(alliance ? BLUE_SCORING_Y - y : RED_SCORING_Y - y, 2)), -1.0, 1.0);

        double angle;
        if(x >= RIGHT_WAYPOINT_X)
            angle = angleToVertex(RIGHT_WAYPOINT_X, alliance ? BLUE_WAYPOINT_Y : RED_WAYPOINT_Y, false);
        else if(x >= LEFT_WAYPOINT_X)
            angle = angleToVertex(LEFT_WAYPOINT_X, alliance ? BLUE_WAYPOINT_Y : RED_WAYPOINT_Y, false);
        else
            angle = angleFromVertex(SCORING_X, alliance ? BLUE_SCORING_Y : RED_SCORING_Y, LEFT_WAYPOINT_X, false);

        drive(power, angle, turn, autoAlign, true);
    }

    /**
     * With the robot at (rx, ry), calculates the drive angle of the robot
     * in order to follow a parabola and arrive at the waypoint (wx, wy)
     * that is the vertex. The parabola is defined to contain the robot's coordinates.
     *
     * @param wx the waypoint x coordinate
     * @param wy the waypoint y coordinate
     * @param toIntake whether the robot is going to the intake
     *
     * @return the drive angle in degrees [-180, 180)
     */
    public double angleToVertex(double wx, double wy, boolean toIntake) {
        if(x == wx)
            return toIntake ? 0.0 : -180.0;

        return MathUtilities.addAngles( Math.toDegrees(Math.atan2(2.0 * (wy - y), wx - x) ), 0.0);
    }

    /**
     * With the robot at (rx, ry), calculates the drive angle of the robot
     * in order to follow a parabola and arrive at the waypoint (wx, wy).
     * The parabola is defined with its vertex constrained to the x-value
     * of h (previous waypoint) and contains both the waypoint and robot
     * coordinates.
     *
     * @param wx the waypoint x coordinate
     * @param wy the waypoint y coordinate
     * @param h  the x value of the previous waypoint
     * @param toIntake whether the robot is going to the intake
     * @return the drive angle in degrees [-180, 180)
     */
    public double angleFromVertex(double wx, double wy, double h, boolean toIntake) {
        double robotDiff = Math.pow(x - h, 2);
        double waypointDiff = Math.pow(wx - h, 2);

        if(x == h)
            return toIntake ? 0.0 : -180.0;
        else if(robotDiff == waypointDiff)
            return y > wy ? -90.0 : 90.0;

        double k = (wy * robotDiff - y * waypointDiff) / (robotDiff - waypointDiff);
        return MathUtilities.addAngles( Math.toDegrees(Math.atan2(2.0 * (k - y), h - x) ), 0.0);
    }

    /**
     * Update the robot's pose using odometry and April Tags.
     * Call at the start of each loop() cycle
     *
     * @param pose the robot's pose [x, y, theta]
     */
    public void updatePose(double[] pose) {
        updateWithOdometry();
        if(pose[0] != -6969)
            updateWithAprilTags(pose);
    }

    /**
     * Updates Pose using Odometry Wheels
     */
    public void updateWithOdometry() {
        int currentLeft = leftDead.getCurrentPosition();
        int currentRight = rightDead.getCurrentPosition();
        int currentCenter = centerDead.getCurrentPosition();

        int deltaLeft = currentLeft - previousLeft;
        int deltaRight = currentRight - previousRight;
        int deltaCenter = currentCenter - previousCenter;

        double deltaHeading = (deltaRight - deltaLeft) * DEGREES_PER_TICK * HEADING_ODOMETRY_CORRECTION;
        heading = MathUtilities.addAngles(heading, deltaHeading);

        double deltaX = deltaCenter * INCHES_PER_TICK * STRAFE_ODOMETRY_CORRECTION;
        double deltaY = (deltaLeft + deltaRight) * .5 * INCHES_PER_TICK * FORWARD_ODOMETRY_CORRECTION;

        double inRadians = Math.toRadians(heading);
        y += -deltaX * Math.cos(inRadians) + deltaY * Math.sin(inRadians);
        x += deltaX * Math.sin(inRadians) + deltaY * Math.cos(inRadians);

        previousLeft = currentLeft;
        previousRight = currentRight;
        previousCenter = currentCenter;
    }

    /**
     * Updates the Robot Pose using April Tags
     *
     * @param pose the pose [x, y, theta]
     */
    private void updateWithAprilTags(double[] pose) {
        this.x = pose[0];
        this.y = pose[1];
        this.heading = pose[2];
    }

    /**
     * Get Motor Velocities for telemetry
     *
     * @return in order: backLeft, backRight, frontLeft, frontRight velocities
     */
    public double[] getMotorVelocities()
    {
        return new double[]{
                backLeft.getVelocity(),
                backRight.getVelocity(),
                frontLeft.getVelocity(),
                frontRight.getVelocity()
        };
    }

    /**
     * Get the dead wheel position values for telemetry
     *
     * @return the dead wheel encoder values in the order:
     * left, right, center
     */
    public double[] getOdometryPositions()
    {
        return new double[]{
                leftDead.getCurrentPosition(),
                rightDead.getCurrentPosition(),
                centerDead.getCurrentPosition()
        };
    }

    /**
     * Get the robot coordinates for telemetry
     *
     * @return the x and y value of the center of the bot
     */
    public double[] getXY() {
        return new double[]{x, y};
    }

    /**
     * Gets the heading of the robot on the field coordinate system
     *
     * @return the heading in degrees [-180, 180)
     */
    public double getFieldHeading() {
        return heading;
    }
}