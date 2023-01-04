package org.firstinspires.ftc.teamcode;

import com.arcrobotics.ftclib.controller.wpilibcontroller.ElevatorFeedforward;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.SimpleServo;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import java.util.List;
import java.util.function.BooleanSupplier;


@TeleOp(name = "Bucket Tele", group = "FtcLib")
//@Disabled
public class RHSBucketTele extends LinearOpMode {
    static final double DRIVE_GEAR_REDUCTION = 1.0;     // No External Gearing.
    static final double WHEEL_DIAMETER_INCHES = 4;
    static final int LOW_JUNCTION = 14;
    static final int MEDIUM_JUNCTION = 24;
    static final int HIGH_JUNCTION = 34;
    static final int HOME_POSITION = 1;
    static final int CONE_HEIGHT = 5;
    static final int ADJUST_ARM_INCREMENT = 1;
    static final double MAX_POWER = 0.4;
    static final double GRIPPER_OPEN = 255;
    static final double GRIPPER_CLOSED = 0;
    static final double GRIPPER_RANGE = 360;

    private MotorEx ArmMotor;
    private ElevatorFeedforward armFeedForward;
    private GamepadEx gamePadArm;
    private GamepadEx gamePadDrive;
    private SimpleServo GripperServo;
    private BooleanSupplier openClaw;
    private BooleanSupplier closeClaw;
    private int armTarget = 0;
    // These are set in init.
    private double countsPerMotorRev = 0;
    private double motorRPM = 0;
    private double countsPerInch = 0;


    public void runOpMode() {
        //TODO This arm name is temporary for testing.
        ArmMotor = new MotorEx(hardwareMap, "leftbackdrive", Motor.GoBILDA.RPM_435);
        armFeedForward = new ElevatorFeedforward(10, 20, 30);
        MotorEx frontLeftDrive = new MotorEx(hardwareMap, "leftfrontdrive", Motor.GoBILDA.RPM_435);
        MotorEx backLeftDrive = new MotorEx(hardwareMap, "leftbackdrive", Motor.GoBILDA.RPM_435);
        MotorEx frontRightDrive = new MotorEx(hardwareMap, "rightfrontdrive", Motor.GoBILDA.RPM_435);
        MotorEx backRightDrive = new MotorEx(hardwareMap, "rightbackdrive", Motor.GoBILDA.RPM_435);
        frontLeftDrive.setInverted(true);
        backLeftDrive.setInverted(true);

        ArmMotor.setInverted(false);
        ArmMotor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        ArmMotor.setPositionCoefficient(.05);
        ArmMotor.setPositionTolerance(10);
        ArmMotor.setRunMode(Motor.RunMode.PositionControl);
        ArmMotor.setTargetPosition(0);
        while (!ArmMotor.atTargetPosition()) {
            ArmMotor.set(1);
        }

        GripperServo = new SimpleServo(hardwareMap, "servo1", 0, GRIPPER_RANGE, AngleUnit.DEGREES);
        openClaw = () -> gamePadArm.wasJustPressed(GamepadKeys.Button.LEFT_BUMPER)
                && !gamePadArm.isDown(GamepadKeys.Button.RIGHT_BUMPER);
        closeClaw = () -> !gamePadArm.isDown(GamepadKeys.Button.LEFT_BUMPER)
                && gamePadArm.wasJustPressed(GamepadKeys.Button.RIGHT_BUMPER);

        // Bulk reads
        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
        // Important: Set all Expansion hubs to use the AUTO Bulk Caching mode
        for (LynxModule module : allHubs) {
            module.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        gamePadDrive = new GamepadEx(gamepad1);
        gamePadArm = new GamepadEx(gamepad2);
//        MecanumDrive drive = new MecanumDrive(frontLeftDrive, frontRightDrive, backLeftDrive, backRightDrive);

        countsPerMotorRev = backLeftDrive.ACHIEVABLE_MAX_TICKS_PER_SECOND;
        motorRPM = backLeftDrive.getMaxRPM();
        countsPerInch = ((countsPerMotorRev * DRIVE_GEAR_REDUCTION) / (WHEEL_DIAMETER_INCHES * 3.1415));

        waitForStart();
        while (opModeIsActive() && !isStopRequested()) {
            gamePadDrive.readButtons();
            gamePadArm.readButtons();
//TODO
//            drive.driveRobotCentric(gamePadDrive.getLeftX(),
//                    gamePadDrive.getLeftY(),
//                    gamePadDrive.getRightX());

            ProcessArm();
            ProcessGripper();

            telemetry.addData("Counts/inch", countsPerInch);
            telemetry.addData("Target Position", "%d", armTarget);
            telemetry.addData("Current Position", ArmMotor.getCurrentPosition());
            telemetry.addData("Servo Position", "%6.2f - %6.2f", GripperServo.getPosition(), GripperServo.getAngle());
            telemetry.update();
        }
    }

    public void ProcessGripper() {
        if (openClaw.getAsBoolean()) {
            GripperServo.turnToAngle(GRIPPER_OPEN);
            moveArm(ArmPosition.ground);
        }

        if (closeClaw.getAsBoolean()) {
            GripperServo.turnToAngle(GRIPPER_CLOSED);
        }
    }

    public void ProcessArm() {
        // Adjust position
        if (gamePadArm.wasJustPressed(GamepadKeys.Button.DPAD_DOWN)) {
            moveArm(ArmPosition.adjustDown);
        }

        if (gamePadArm.wasJustPressed(GamepadKeys.Button.DPAD_UP)) {
            moveArm(ArmPosition.adjustUp);
        }

        // Low junction
        if (gamePadArm.wasJustPressed(GamepadKeys.Button.A)) {
            moveArm(ArmPosition.low);
        }

        // medium junction
        if (gamePadArm.wasJustPressed(GamepadKeys.Button.B)) {
            moveArm(ArmPosition.medium);
        }

        // high junction
        if (gamePadArm.wasJustPressed(GamepadKeys.Button.Y)) {
            moveArm(ArmPosition.high);
        }

        // ground junction
        if (gamePadArm.wasJustPressed(GamepadKeys.Button.X)) {
            moveArm(ArmPosition.ground);
        }
    }

    public void moveArm(ArmPosition position) {
        int currentPosition = ArmMotor.getCurrentPosition();
        switch (position) {
            case ground:
                armTarget = HOME_POSITION * (int) countsPerInch;
                break;
            case low:
                armTarget = (LOW_JUNCTION * (int) countsPerInch);
                break;
            case medium:
                armTarget = (MEDIUM_JUNCTION * (int) countsPerInch);
                break;
            case high:
                armTarget = (HIGH_JUNCTION * (int) countsPerInch);
                break;
            case adjustUp:
                armTarget = currentPosition + (ADJUST_ARM_INCREMENT * (int) countsPerInch);
                break;
            case adjustDown:
                armTarget = currentPosition - (ADJUST_ARM_INCREMENT * (int) countsPerInch);
                break;
            default:
                armTarget = 0;
        }

        // Prevent arm moving below HOME_POSITION
        armTarget = Math.max(armTarget, HOME_POSITION);
        ArmMotor.setRunMode(Motor.RunMode.PositionControl);
        ArmMotor.setTargetPosition(armTarget);

        while (!ArmMotor.atTargetPosition()) {
            ArmMotor.set(armFeedForward.calculate(MAX_POWER));
        }

        ArmMotor.stopMotor();
    }

    public enum ArmPosition {
        ground,
        low,
        medium,
        high,
        adjustUp,
        adjustDown
    }
}