
package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.TalonFXFeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX; //librerias
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import com.kauailabs.navx.frc.AHRS;


import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.Controles;
import frc.robot.Constants.KPIDShooter;
import frc.robot.Constants.Kxbox;
import frc.robot.Constants.Motores;
import frc.robot.Constants.Neumatica;
import frc.robot.Constants.VelocidadChasis;
import frc.robot.Constants.statusrobot;
import frc.robot.Constants.velocidadesShooter;



public class Robot extends TimedRobot {

  // CHASIS //
  WPI_TalonSRX MOTORD1ENC = new WPI_TalonSRX(Motores.Chasis.KMOTORD1);
  WPI_TalonSRX MOTORD2 = new WPI_TalonSRX(Motores.Chasis.KMOTORD2);
  WPI_TalonSRX MOTORD3 = new WPI_TalonSRX(Motores.Chasis.KMOTORD3);
  WPI_TalonSRX MOTORI4ENC = new WPI_TalonSRX(Motores.Chasis.KMOTORI4);
  WPI_TalonSRX MOTORI5 = new WPI_TalonSRX(Motores.Chasis.KMOTORI5);
  WPI_TalonSRX MOTORI6 = new WPI_TalonSRX(Motores.Chasis.KMOTORI6);
  MotorControllerGroup MOTSI = new MotorControllerGroup(MOTORD1ENC, MOTORD2, MOTORD3);
  MotorControllerGroup MOTSD = new MotorControllerGroup(MOTORI4ENC, MOTORI5, MOTORI6);
  DifferentialDrive chasis = new DifferentialDrive(MOTSI, MOTSD);
  DoubleSolenoid PISTCHASIS = new DoubleSolenoid(PneumaticsModuleType.CTREPCM, Neumatica.KPISTCHASIS1,
      Neumatica.KPISTCHASIS2);

  // Neumática // (los pistones estan en su respectivo mecanismo)
  Compressor COMPRESOR = new Compressor(0, PneumaticsModuleType.CTREPCM);

  // CONTROLES //
  Joystick JoystickDriver1 = new Joystick(Controles.kJoystickDriver1);
  Joystick JoystickDriver2 = new Joystick(Controles.KJoystickDriver2);

  // INTAKE //
  DoubleSolenoid PISTINTAKE = new DoubleSolenoid(PneumaticsModuleType.CTREPCM, Neumatica.KPISTINTAKE1,
      Neumatica.KPISTINTAKE2);
  WPI_VictorSPX MOTORINTAKE = new WPI_VictorSPX(Motores.Intake.KMOTORINTAKE);
  boolean motints = false;

  // SHOOTER //
  WPI_TalonFX MOTORSHOOTERLEFT = new WPI_TalonFX(Motores.Shooter.KMOTORSLeft);
  WPI_TalonFX MOTORSHOOTERRIGHT = new WPI_TalonFX(Motores.Shooter.KMOTORSRight);

  StringBuilder _sbshoot = new StringBuilder(); /* String for output(PID) */

  // INDEXER //
  WPI_VictorSPX MOTORINDEXER = new WPI_VictorSPX(Motores.Indexer.KMOTORINDEXER);
  DigitalInput limitindexer = new DigitalInput(Constants.LimitSwitches.indexer);

  // CAPUCHA //
  WPI_TalonSRX MOTORCAPUCHA = new WPI_TalonSRX(Motores.Capucha.KMOTORCAPUCHA);
  DigitalInput limitcapucha = new DigitalInput(Constants.LimitSwitches.capucha);
  boolean _lastButton1 = false;
  double targetPositionRotations;
  StringBuilder _sbcapucha = new StringBuilder(); /* String for output(PID) */

  double AnguloCapuchaConfig;

  // CLIMBER //
  //CANSparkMax MOTORCLIMBER = new CANSparkMax(Climber.KMOTORCLIMBER, MotorType.kBrushless);

  PowerDistribution pdp = new PowerDistribution();

  // LIMELIGHT //////
  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx");
  NetworkTableEntry ty = table.getEntry("ty");
  NetworkTableEntry ta = table.getEntry("ta");

  


  // ¿Cuántos grados hacia atrás gira su centro de atención desde la posición
  // perfectamente vertical?
  double anguloInclinacionLL = 39.5;
  // distancia desde el centro de la lente Limelight hasta el suelo
  double alturaAlPisoPugadasLL = 22.4;
  // distancia del objetivo al suelo
  double alturaUpperPulgadas = 105.1 ; // distancia hub 103.9 in
 

  // Navx /////
  AHRS navx = new AHRS(SPI.Port.kMXP);

  // ESTRATEGIA AUTOAPUNTADO //
  double ktick2Degree = 56.88;
  double capuchavalor;
  double capucha_angulo;
  double anguloFinal;


//autonomo

double KPgiro=0.0055;
double headin;

  /*
   *
   * SEPARACION DE INSTANCIAS
   *
   */

  @Override
  public void robotInit() {
    reiniciarSensores();
    //CameraServer.startAutomaticCapture();

  }

  @Override
  public void robotPeriodic() {

    resetLimitSwitch();

    // Calculos
    double x = tx.getDouble(0.0);
    double y = ty.getDouble(0.0);
    double area = ta.getDouble(0.0);
    capuchavalor = MOTORCAPUCHA.getSelectedSensorPosition();
    anguloFinal = -1 * capuchavalor / ktick2Degree;
    double angleToGoalDegrees = anguloInclinacionLL + y;
    double angleToGoalRadians = angleToGoalDegrees * (3.14159 / 180.0);
    double distanciaFender = (alturaUpperPulgadas - alturaAlPisoPugadasLL)/Math.tan(angleToGoalRadians);


    
    // IMPRIME LOS VALORES EN EL SMARTDASHBOARD

    SmartDashboard.putBoolean("Intake", !statusrobot.IntakeState);
    SmartDashboard.putBoolean("Compresor", !statusrobot.compresorState);
    SmartDashboard.putNumber("RPM shooter", MOTORSHOOTERRIGHT.getSelectedSensorVelocity()*-1*10*60/2048);
    SmartDashboard.putNumber("Current Shooter", (pdp.getCurrent(12)+pdp.getCurrent(3))/2);
    SmartDashboard.putBoolean("Reset Capucha", limitcapucha.get());
    SmartDashboard.putNumber("Valor Capucha", MOTORCAPUCHA.getSelectedSensorPosition());
    SmartDashboard.putNumber("Corriente Capucha", MOTORCAPUCHA.getSupplyCurrent());
    SmartDashboard.putNumber("Angulo Capucha", anguloFinal);
    SmartDashboard.putNumber("LL X Value", x);
    SmartDashboard.putNumber("LL Y Value", y);
    SmartDashboard.putNumber("LL X Area", area);
    SmartDashboard.putNumber("Distancia Fender", distanciaFender);
    

  SmartDashboard.putNumber("angulochasis", navx.getAngle());
    
  }

  @Override
  public void autonomousInit() {
    reiniciarSensores();
    desactivartodo();
    falconpidConfig();
    capuchaPIDinit();

  }

  @Override
  public void autonomousPeriodic() { // Autonomo
    //ajusteDeTiroautonomo();

double error=120-navx.getAngle();

chasis.arcadeDrive(-KPgiro*error,0);


    /*if (Timer.getMatchTime() <= 15 && Timer.getMatchTime() >= 10) {
      ShooterPID(-5000);
      AnguloCapuchaConfig = 6.8;

    } else {
      MOTORSHOOTERLEFT.set(0);
      MOTORSHOOTERRIGHT.set(0);
    }

    if (Timer.getMatchTime() <= 12 && Timer.getMatchTime() >= 9.8) {
      MOTORINDEXER.set(0.4);
    } else {
      MOTORINDEXER.set(0);
    }

    if (Timer.getMatchTime() <= 9 ) {
     // AutonomoTaxi();
    } */
    }

  

  @Override
  public void teleopInit() {

    falconpidConfig();
    reiniciarSensores();
    desactivartodo();
    // PIDchasis();
    capuchaPIDinit();

    AnguloCapuchaConfig = 0;

  }

  @Override
  public void teleopPeriodic() { // Teleoperado

    if (JoystickDriver1.getRawButton(Kxbox.BOTONES.A)) {
      chasis_shoot_Adjust();
    } else {
      // Mover Chassis
      double velocidad = JoystickDriver1.getRawAxis(Kxbox.AXES.RT) - JoystickDriver1.getRawAxis(Kxbox.AXES.LT);
      chasis.arcadeDrive(
          -VelocidadChasis.velocidadgiro * JoystickDriver1.getRawAxis(Kxbox.AXES.joystick_izquierdo_eje_X),
          -VelocidadChasis.velocidadX * -velocidad);
    }
    // Intake
    compresor();
    Intake();
    returnHome();
    //climbler();
    anguloyvelocidad();
  }

  @Override
  public void disabledInit() {
    reiniciarSensores();
    desactivartodo();
    navx.reset();

  }

  @Override
  public void disabledPeriodic() {
    desactivartodo();

    if (limitcapucha.get() == true) {
      MOTORCAPUCHA.set(-0.4);
    } else {
      MOTORCAPUCHA.set(0);
    }
  }

  @Override
  public void testInit() {

   // falconpidConfig();

  }

  @Override
  public void testPeriodic() {


  
    //ShooterPID(-5000);
  }

  /*
   *
   *
   * // SEPARACION DE PERIODOS//
   *
   *
   */

  public void compresor() {

    // SE PRENDE EL COMPRESOR CON EL BOTON "B"
    // Mas adelante cambiar esto al driver secundario

    if (JoystickDriver1.getRawButtonPressed(Kxbox.BOTONES.boton_con_lineas)) {
      if (statusrobot.compresorState) {
        COMPRESOR.enableDigital();
        statusrobot.compresorState = false;
      } else {
        COMPRESOR.disable();
        statusrobot.compresorState = true;
      }
    }
  }

  public void Intake() {

    /*
     * if (JoystickDriver1.getRawButtonPressed(Kxbox.BOTONES.A)) {
     * if (statusrobot.IntakeState) {
     * PISTINTAKE.set(true);
     * statusrobot.IntakeState = false;
     * 
     * }
     * } else {
     * PISTINTAKE.set(false);
     * //MOTORINTAKE.set(-0);
     * 
     * statusrobot.IntakeState = true;
     * 
     * {
     */

    if (JoystickDriver1.getRawButton(Kxbox.BOTONES.LB)) {

      PISTINTAKE.set(Value.kReverse);
      MOTORINTAKE.set(0);

    }

    if (JoystickDriver1.getRawButton(Kxbox.BOTONES.RB)) {
      PISTINTAKE.set(Value.kForward);
      MOTORINTAKE.set(0.45);

    }

    if (JoystickDriver1.getRawButton(Kxbox.BOTONES.X) == true) {

      MOTORINTAKE.set(-0.45);

    }

    if (JoystickDriver1.getRawButton(Kxbox.BOTONES.B) == true) {

      MOTORINTAKE.set(0.45);

    }

    if (JoystickDriver1.getRawButton(Kxbox.BOTONES.Y) == true) {

      MOTORINTAKE.set(-0);

    }

  }

  public void desactivartodo() {

    // Desactiva totalmente todo, incluso si ya estaba desactivado antes
    chasis.arcadeDrive(0, 0);
    PISTCHASIS.set(Value.kOff);
    PISTINTAKE.set(Value.kReverse);
    MOTORINTAKE.set(0);
    MOTORINDEXER.set(0);
    MOTORSHOOTERLEFT.set(0);
    MOTORSHOOTERRIGHT.set(0);
    MOTORCAPUCHA.set(0);
    //MOTORCLIMBER.set(0);

  }

  public void reiniciarSensores() {

    // Reset de sensores de encoders, navx.
    MOTORD1ENC.setSelectedSensorPosition(0);
    MOTORI4ENC.setSelectedSensorPosition(0);
    COMPRESOR.disable();
    statusrobot.IntakeState = false;
    statusrobot.compresorState = false;
    velocidadesShooter.velocidad = 0;
    AnguloCapuchaConfig = 0;

    MOTORCAPUCHA.set(0);

  }

  /*
   * public void cambiosShifter() {
   * 
   * if (JoystickDriver1.getPOV() == ControlarMecanismos.shifter1) {
   * PISTCHASIS.set(Value.kForward);
   * }
   * if (JoystickDriver1.getPOV() == ControlarMecanismos.shifter2) {
   * PISTCHASIS.set(Value.kReverse);
   * }
   * }
   */
  public void AutonomoTaxi() { // Se mueve :) Pa delante

    double encoIzq = MOTORI4ENC.getSelectedSensorPosition();
    SmartDashboard.putNumber("Econder izquierdo", encoIzq);
    double encoDer = MOTORD1ENC.getSelectedSensorPosition();
    double testencodermenos = -1 * encoDer;
    SmartDashboard.putNumber("Econder derecho", testencodermenos);

    // encoizq

    double vuelta = encoIzq / 4096 / 4.17;
    double distanciainches = vuelta * 6.1 * Math.PI; // Units.inchesToMeters(3.2 );
    double distmeters = Units.inchesToMeters(distanciainches);

    SmartDashboard.putNumber("distancia?", distmeters);

    if (distmeters <= 2.4) {
      chasis.arcadeDrive(0, -0.5);
    }

    if (distmeters >= 2.41 && distmeters <= 2.6) {
      chasis.arcadeDrive(0, 0);
    }

    if (distmeters >= 2.61) {
      chasis.arcadeDrive(0, 0.5);
    }

    /*double direccionx = navx.getDisplacementX();
    double direcciony = navx.getDisplacementX();
    double angulo = navx.getAngle();

    SmartDashboard.putNumber("Coordenada x", direccionx);
    SmartDashboard.putNumber("Coordenada y", direcciony);
    SmartDashboard.putNumber("angulo", angulo);

    
      if (distmeters <= 3 && angulo <= 5) {
      chasis.arcadeDrive(-0.5, 0.7);
      }
      if (distmeters <= 3 && angulo >= 5) {
      chasis.arcadeDrive(0.4, 0.7);
      }
      
      if (distmeters <= 3 && angulo <= 5 && angulo >= -5) {
      chasis.arcadeDrive(-0, 0.7);
     }*/
     

  }

  public void falconpidConfig() { // no moverle a esto por favor👍

    /* Factory Default all hardware to prevent unexpected behaviour */
    MOTORSHOOTERLEFT.configFactoryDefault();

    /* Config neutral deadband to be the smallest possible */
    MOTORSHOOTERLEFT.configNeutralDeadband(0.001);

    /* Config sensor used for Primary PID [Velocity] */
    MOTORSHOOTERLEFT.configSelectedFeedbackSensor(TalonFXFeedbackDevice.IntegratedSensor,
        Constants.KPIDShooter.kPIDLoopIdx,
        Constants.KPIDShooter.kTimeoutMs);

    /* Config the peak and nominal outputs */
    MOTORSHOOTERLEFT.configNominalOutputForward(0, Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERLEFT.configNominalOutputReverse(0, Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERLEFT.configPeakOutputForward(1, Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERLEFT.configPeakOutputReverse(-1, Constants.KPIDShooter.kTimeoutMs);

    /* Config the Velocity closed loop gains in slot0 */
    MOTORSHOOTERLEFT.config_kF(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kF,
        Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERLEFT.config_kP(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kP,
        Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERLEFT.config_kI(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kI,
        Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERLEFT.config_kD(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kD,
        Constants.KPIDShooter.kTimeoutMs);

    MOTORSHOOTERLEFT.configOpenloopRamp(1.5);

    // https://phoenix-documentation.readthedocs.io/en/latest/ch14_MCSensor.html#

    /* Factory Default all hardware to prevent unexpected behaviour */
    MOTORSHOOTERRIGHT.configFactoryDefault();

    /* Config neutral deadband to be the smallest possible */
    MOTORSHOOTERRIGHT.configNeutralDeadband(0.001);

    /* Config sensor used for Primary PID [Velocity] */
    MOTORSHOOTERRIGHT.configSelectedFeedbackSensor(TalonFXFeedbackDevice.IntegratedSensor,
        Constants.KPIDShooter.kPIDLoopIdx,
        Constants.KPIDShooter.kTimeoutMs);

    /* Config the peak and nominal outputs */
    MOTORSHOOTERRIGHT.configNominalOutputForward(0, Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERRIGHT.configNominalOutputReverse(0, Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERRIGHT.configPeakOutputForward(1, Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERRIGHT.configPeakOutputReverse(-1, Constants.KPIDShooter.kTimeoutMs);

    /* Config the Velocity closed loop gains in slot0 */
    MOTORSHOOTERRIGHT.config_kF(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kF,
        Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERRIGHT.config_kP(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kP,
        Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERRIGHT.config_kI(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kI,
        Constants.KPIDShooter.kTimeoutMs);
    MOTORSHOOTERRIGHT.config_kD(Constants.KPIDShooter.kPIDLoopIdx, Constants.KPIDShooter.kGains_Velocit.kD,
        Constants.KPIDShooter.kTimeoutMs);

    MOTORSHOOTERRIGHT.configOpenloopRamp(1.4);

    // https://phoenix-documentation.readthedocs.io/en/latest/ch14_MCSensor.html#

  }

  public void ShooterPID(double rpmtotal) { // 6380 maximo

    // https://phoenix-documentation.readthedocs.io/en/latest/ch14_MCSensor.html#
    double rpmconv = KPIDShooter.torpm * rpmtotal;
    double valor = -1 * rpmconv;// JoystickDriver1.getRawAxis(Kxbox.AXES.joystick_derecho_eje_Y);

    SmartDashboard.putNumber("conv", rpmconv);

    double targetVelocity_UnitsPer100ms = valor * 3000 * 2048.0 / 600.0;
    _sbshoot.setLength(0);
    MOTORSHOOTERLEFT.set(TalonFXControlMode.Velocity, targetVelocity_UnitsPer100ms);

    MOTORSHOOTERRIGHT.set(TalonFXControlMode.Velocity, -targetVelocity_UnitsPer100ms);
  }

  public void ajustedegiro() { // Probar
    double velocidad = JoystickDriver1.getRawAxis(Kxbox.AXES.RT) - JoystickDriver1.getRawAxis(Kxbox.AXES.LT);

    double x = tx.getDouble(0.0);
    double ajusteGiro = 0.0f;
    float min_command = 0.03f;

    if (x > 0.2) {

      ajusteGiro = Constants.LimeLight.kp * x - min_command;

    } else if (x < 0.2) {

      ajusteGiro = Constants.LimeLight.kp * x + min_command;

    }
    double y = ty.getDouble(0.0);
    double angleToGoalDegrees = anguloInclinacionLL + y;
    double angleToGoalRadians = angleToGoalDegrees * (3.14159 / 180.0);
    double distanciaFender = (alturaUpperPulgadas - alturaAlPisoPugadasLL)/Math.tan(angleToGoalRadians);


if(distanciaFender<100){

  chasis.arcadeDrive(ajusteGiro, -0.5);

}
if(distanciaFender>105){

  chasis.arcadeDrive(ajusteGiro, 0.5);
}

if(distanciaFender>100&&distanciaFender<104){
chasis.arcadeDrive(ajusteGiro, 0);

}
  }

  public void chasis_shoot_Adjust() { 
    
    double x = tx.getDouble(0.0);
    double ajusteGiro = 0.0f;
    float min_command = 0.03f;

    if (x > 0.2) {

      ajusteGiro = Constants.LimeLight.kp * x - min_command;

    } else if (x < 0.2) {

      ajusteGiro = Constants.LimeLight.kp * x + min_command;

    }


    // Probar
    double y = ty.getDouble(0.0);
    double angleToGoalDegrees = anguloInclinacionLL + y;
    double angleToGoalRadians = angleToGoalDegrees * (3.14159 / 180.0);
    double distanciaFender = (alturaUpperPulgadas - alturaAlPisoPugadasLL)/Math.tan(angleToGoalRadians);


if(distanciaFender<67){

  chasis.arcadeDrive(ajusteGiro, -0.45);

}
if(distanciaFender>70){

  chasis.arcadeDrive(ajusteGiro, 0.45);
}

if(distanciaFender>67&&distanciaFender<70){
chasis.arcadeDrive(ajusteGiro, 0);

}
  }

  
/*
  public void climbler() { // Probar

    if (JoystickDriver2.getPOV() == Kxbox.POV.arriba) {

      MOTORCLIMBER.set(0.7);
    }
    if (JoystickDriver2.getPOV() == Kxbox.POV.abajo) {

      MOTORCLIMBER.set(-1);
    } else if (JoystickDriver2.getPOV() == -1) {
      MOTORCLIMBER.set(0);
    }

  }
*/

  public void returnHome() {

    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.RB)) {
      AnguloCapuchaConfig = 0;
    }
  }

  public void resetLimitSwitch() {
    if (limitcapucha.get() == false) {
      MOTORCAPUCHA.setSelectedSensorPosition(0);
    }
  }

  public void capuchaPIDinit() {
    /* Factory Default all hardware to prevent unexpected behaviour */
    MOTORCAPUCHA.configFactoryDefault();

    /* Config sensor used for Primary PID [Velocity] */
    MOTORCAPUCHA.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative,
        Constants.KPIDCapucha.kPIDLoopIdx,
        Constants.KPIDCapucha.kTimeoutMs);

    /**
     * Phase sensor accordingly.
     * Positive Sensor Reading should match Green (blinking) Leds on Talon
     */
    MOTORCAPUCHA.setSensorPhase(true);

    /* Config the peak and nominal outputs */
    MOTORCAPUCHA.configNominalOutputForward(0, Constants.KPIDCapucha.kTimeoutMs);
    MOTORCAPUCHA.configNominalOutputReverse(0, Constants.KPIDCapucha.kTimeoutMs);
    MOTORCAPUCHA.configPeakOutputForward(1, Constants.KPIDCapucha.kTimeoutMs);
    MOTORCAPUCHA.configPeakOutputReverse(-1, Constants.KPIDCapucha.kTimeoutMs);
    /* Config the Velocity closed loop gains in slot0 */
    MOTORCAPUCHA.config_kF(Constants.KPIDCapucha.kPIDLoopIdx, Constants.KPIDCapucha.kGains_Velocit.kF,
        Constants.KPIDCapucha.kTimeoutMs);
    MOTORCAPUCHA.config_kP(Constants.KPIDCapucha.kPIDLoopIdx, Constants.KPIDCapucha.kGains_Velocit.kP,
        Constants.KPIDCapucha.kTimeoutMs);
    MOTORCAPUCHA.config_kI(Constants.KPIDCapucha.kPIDLoopIdx, Constants.KPIDCapucha.kGains_Velocit.kI,
        Constants.KPIDCapucha.kTimeoutMs);
    MOTORCAPUCHA.config_kD(Constants.KPIDCapucha.kPIDLoopIdx, Constants.KPIDCapucha.kGains_Velocit.kD,
        Constants.KPIDCapucha.kTimeoutMs);

  }

  public void capuchaPIDteleop(double rpmcapucha) {

    // https://phoenix-documentation.readthedocs.io/en/latest/ch14_MCSensor.html#
    double rpmconve = KPIDShooter.torpm * rpmcapucha;
    double valor = -1 * rpmconve;

    SmartDashboard.putNumber("conv", rpmconve);

    double targetVelocity_UnitsPer100ms = valor * 3000 * 2048.0 / 600.0;
    MOTORCAPUCHA.set(ControlMode.Velocity, targetVelocity_UnitsPer100ms);
    _sbshoot.setLength(0);

  }

  public void anguloyvelocidad() {

    // Apuntar
    if (JoystickDriver2.getRawAxis(Kxbox.AXES.LT) >= 0.5) {
      ShooterPID(velocidadesShooter.velocidad);
    } else {
      MOTORSHOOTERLEFT.set(0);
      MOTORSHOOTERRIGHT.set(0);
    }

    // Disparar
    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.LB) && limitindexer.get() == true) {
      MOTORINDEXER.set(0.3);
    } else if (JoystickDriver2.getRawAxis(Kxbox.AXES.RT) >= 0.5) {
      MOTORINDEXER.set(0.3);
    } else {
      MOTORINDEXER.set(0);
    }

    // Capucha
    if (anguloFinal >= (-AnguloCapuchaConfig + 1)) {
      MOTORCAPUCHA.set(0.3);
    } else if ((anguloFinal >= (-AnguloCapuchaConfig - 1)) && (anguloFinal <= (-AnguloCapuchaConfig + 1))) {
      MOTORCAPUCHA.set(0);
    } else if (anguloFinal <= (-AnguloCapuchaConfig - 1)) {
      MOTORCAPUCHA.set(-0.3);
    }

    // Tiro Fender
    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.B)) {
      velocidadesShooter.velocidad = velocidadesShooter.fender; // 4650
      AnguloCapuchaConfig = Constants.anguloCapucha.fender;
    }

    // Tarmac 1.84m
    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.A)) {
      AnguloCapuchaConfig =  Constants.anguloCapucha.tarmac;
      velocidadesShooter.velocidad = velocidadesShooter.tarmac;
    }

    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.RB)) {
      AnguloCapuchaConfig = 0;
    }

    // Launch Pad
    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.X)) {
      AnguloCapuchaConfig =  Constants.anguloCapucha.launchpad;
      velocidadesShooter.velocidad = velocidadesShooter.launchpad;
    }

  }

  public void ajusteDeTiroautonomo() {
    if (anguloFinal >= (-AnguloCapuchaConfig + 1)) {

      MOTORCAPUCHA.set(0.25);
    } else if ((anguloFinal >= (-AnguloCapuchaConfig - 1)) && (anguloFinal <= (-AnguloCapuchaConfig + 1))) {

      MOTORCAPUCHA.set(0);
    } else if (anguloFinal <= (-AnguloCapuchaConfig - 1)) {

      MOTORCAPUCHA.set(-0.25);

    }

  }

  public void PIDchasis() {

    MOTORD1ENC.configFactoryDefault();
    MOTORD2.configFactoryDefault();

    MOTORD3.configFactoryDefault();
    MOTORI4ENC.configFactoryDefault();
    MOTORI5.configFactoryDefault();
    MOTORI6.configFactoryDefault();



    MOTORD1ENC.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative);
    MOTORI4ENC.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative);

    MOTORD1ENC.configOpenloopRamp(0.0);
    MOTORI4ENC.configOpenloopRamp(0.0);
  }

}