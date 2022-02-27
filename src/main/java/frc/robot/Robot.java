
package frc.robot;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.Controllers;
import frc.robot.Constants.Motores;
import frc.robot.Constants.Neumatica;
import frc.robot.Constants.statusrobot;
import frc.robot.Constants.velocidades;

public class Robot extends TimedRobot {

  // CHASIS//
  WPI_TalonSRX MOTORD1ENC = new WPI_TalonSRX(Motores.KMOTORD1);
  WPI_TalonSRX MOTORD2 = new WPI_TalonSRX(Motores.KMOTORD2);
  WPI_TalonSRX MOTORD3 = new WPI_TalonSRX(Motores.KMOTORD3);
  WPI_TalonSRX MOTORI4ENC = new WPI_TalonSRX(Motores.KMOTORI4);
  WPI_TalonSRX MOTORI5 = new WPI_TalonSRX(Motores.KMOTORI5);
  WPI_TalonSRX MOTORI6 = new WPI_TalonSRX(Motores.KMOTORI6);
  MotorControllerGroup MOTSI = new MotorControllerGroup(MOTORD1ENC, MOTORD2, MOTORD3);
  MotorControllerGroup MOTSD = new MotorControllerGroup(MOTORI4ENC, MOTORI5, MOTORI6);
  DifferentialDrive chasis = new DifferentialDrive(MOTSI, MOTSD);
  Solenoid PISTCHASIS = new Solenoid(PneumaticsModuleType.CTREPCM, Neumatica.KPISTCHASIS);

  // Neumática
  Compressor COMPRESOR = new Compressor(0, PneumaticsModuleType.CTREPCM);

  // CONTROLES
  Joystick JoystickDriver1 = new Joystick(Controllers.kJoystickDriver1);
  Joystick JoystickDriver2 = new Joystick(Controllers.KJoystickDriver2);

  // INTAKE
  Solenoid PISTINTAKE = new Solenoid(PneumaticsModuleType.CTREPCM, Neumatica.KPISTINTAKE);
  WPI_TalonSRX MOTORINTAKE = new WPI_TalonSRX(Motores.KMOTORIN);

  // Navx
  AHRS navx = new AHRS(SPI.Port.kMXP);

  @Override
  public void robotInit() {

  }

  @Override
  public void robotPeriodic() {

    // IMPRIME LOS VALORES EN EL SMARTDASHBOARD
    SmartDashboard.putBoolean("Intake", statusrobot.IntakeState);
    SmartDashboard.putBoolean("Compresor", statusrobot.compresorState);

  }

  @Override
  public void autonomousInit() {
  }

  @Override
  public void autonomousPeriodic() {
  }

  @Override
  public void teleopInit() {
  }

  @Override
  public void teleopPeriodic() {

    manejarchasis();
    compresorbotonB();
    IntakeBotA();
    cambiosShifter();

  }

  @Override
  public void disabledInit() {
  }

  @Override
  public void disabledPeriodic() {
    desactivartodo();
    reiniciarSensores();
  }

  @Override
  public void testInit() {
  }

  @Override
  public void testPeriodic() {

  }

  // SEPARACION DE PERIODOS//

  public void manejarchasis() {

    // Movimiento del chasis con control Xbox
    velocidades.velocidad = velocidades.velocidadX * JoystickDriver1.getRawAxis(3) - JoystickDriver1.getRawAxis(2);
    chasis.arcadeDrive(velocidades.velocidad, velocidades.velocidadgiro * JoystickDriver1.getRawAxis(0));

  }

  public void compresorbotonB() {

    // SE PRENDE EL COMPRESOR CON EL BOTON "B"
    // Mas adelante cambiar esto al driver secundario
    if (JoystickDriver1.getRawButton(1)) {
      if (statusrobot.compresorState = true) {
        PISTINTAKE.set(true);
      } else {
        PISTINTAKE.set(false);
      }
      statusrobot.compresorState = !statusrobot.compresorState;
    }
  }

  public void IntakeBotA() {

    // ACCIONAMIENTO DE INTAKE CON BOTON "A"

    if (JoystickDriver1.getRawButton(0)) {
      if (statusrobot.IntakeState = true) {
        PISTINTAKE.set(true);
        MOTORINTAKE.set(0.2);
      } else {
        PISTINTAKE.set(false);
        MOTORINTAKE.set(0);

      }
      statusrobot.IntakeState = !statusrobot.IntakeState;
    }

  }

  public void desactivartodo() {  

    //Desactiva totalmente todo, incluso si ya estaba desactivado antes
    chasis.arcadeDrive(0, 0);
    PISTCHASIS.set(false);
    PISTINTAKE.set(false);
  }

  public void reiniciarSensores() {

    //Reset de sensores de encoders, navx.
    MOTORD1ENC.setSelectedSensorPosition(0);
    MOTORI4ENC.setSelectedSensorPosition(0);
    navx.reset();

  }

  public void cambiosShifter() {

    if (JoystickDriver1.getPOV() == 0) {
      PISTINTAKE.set(false);
    }
    if (JoystickDriver1.getPOV() == 180) {
      PISTINTAKE.set(true);
    }
  }


}