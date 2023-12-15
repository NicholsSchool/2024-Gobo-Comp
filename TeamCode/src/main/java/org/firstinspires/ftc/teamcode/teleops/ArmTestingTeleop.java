package org.firstinspires.ftc.teamcode.teleops;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.Arm;
import org.firstinspires.ftc.teamcode.utils.Constants;

/**
 * A teleop for testing Arm functionalities using
 * FTC Dashboard
 */
@Config
@TeleOp(name="[DASHBOARD] Arm Testing")
public class ArmTestingTeleop extends OpMode implements Constants
{
    public Arm arm;
    public static double shoulderPower;
    public static boolean climb;
    public static boolean unClimb;
    public static double extensionPosition;
    public static boolean launchPlane;
    public static double desiredAngle;
    public static boolean ARM_PID;
    public static int desiredWristAngle;
    public static boolean fourbar;

    @Override
    public void init() {
        arm = new Arm(hardwareMap);
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
    }

    @Override
    public void loop() {

        if(fourbar)
            arm.fourbar();
        else
            arm.setWristPos(desiredWristAngle);

        if(ARM_PID)
            arm.armGoToPos(desiredAngle);
        else
            arm.armManualControl(shoulderPower);

        if(climb)
            arm.winchRobot();
        else if(unClimb)
            arm.winchOpposite();
        else
            arm.stopWinch();

        arm.setExtensionManual(extensionPosition);
        arm.setPlaneLauncher(launchPlane);
        telemetry.addData("shoulderPower", shoulderPower);
        telemetry.addData("extensionPosition", extensionPosition);
        telemetry.addData("launchPlane", launchPlane);
        telemetry.addData("angle", arm.getArmAngle() );
        telemetry.addData("desired angle", desiredAngle );
        telemetry.addData("pot", arm.getPot());
        telemetry.addData("wrist pos", arm.getWristAngle());
        telemetry.update();
    }
}
