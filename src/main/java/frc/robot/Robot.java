
package frc.robot;

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
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.ControlarMecanismos;
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
  Solenoid PISTINTAKE = new Solenoid(PneumaticsModuleType.CTREPCM, Neumatica.KPISTINTAKE);
  WPI_VictorSPX MOTORINTAKE = new WPI_VictorSPX(Motores.Intake.KMOTORINTAKE);
  boolean motints = false;

  // SHOOTER //
  WPI_TalonFX MOTORSHOOTERLEFT = new WPI_TalonFX(Motores.Shooter.KMOTORSLeft);
  WPI_TalonFX MOTORSHOOTERRIGHT = new WPI_TalonFX(Motores.Shooter.KMOTORSRight);

  StringBuilder _sb = new StringBuilder(); /* String for output(PID) */

  // INDEXER //
  WPI_VictorSPX MOTORINDEXER = new WPI_VictorSPX(Motores.Indexer.KMOTORINDEXER);

  // CAPUCHA //
  WPI_TalonSRX MOTORCAPUCHA = new WPI_TalonSRX(Motores.Capucha.KMOTORCAPUCHA);
  DigitalInput limitcapucha = new DigitalInput(Constants.LimitSwitches.capucha);
  boolean _lastButton1 = false;
  double targetPositionRotations;


  // CLIMBER //
  WPI_TalonSRX MOTORCLIMBER = new WPI_TalonSRX(Motores.Climber.KMOTORCLIMBER);

  // LIMELIGHT //////
  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx");
  NetworkTableEntry ty = table.getEntry("ty");
  NetworkTableEntry ta = table.getEntry("ta");

  double targetOffsetAngle_Vertical = ty.getDouble(0.0);

  // ¿Cuántos grados hacia atrás gira su centro de atención desde la posición
  // perfectamente vertical?
  double limelightMountAngleDegrees = 25.0;

  // distancia desde el centro de la lente Limelight hasta el suelo
  double limelightHeightInches = 20.0;

  // distancia del objetivo al suelo
  double goalHeightInches = 103.9; // distancia hub 103.9 in

  double angleToGoalDegrees = limelightMountAngleDegrees + targetOffsetAngle_Vertical;
  double angleToGoalRadians = angleToGoalDegrees * (3.14159 / 180.0);

  // Navx /////
  AHRS navx = new AHRS(SPI.Port.kMXP);

  // ESTRATEGIA AUTOAPUNTADO //
  double ktick2Degree = 56.88;
  double capuchavalor;
  double capucha_angulo;
  double anguloFinal;


  /*
   *
   * SEPARACION DE INSTANCIAS
   *
   */

  @Override
  public void robotInit() {
    reiniciarSensores();
    desactivartodo();

  }

  @Override
  public void robotPeriodic() {

    resetLimitSwitch();
    // Calculos
    double distanceFromLimelightToGoalInches = (goalHeightInches - limelightHeightInches)
        / Math.tan(angleToGoalRadians);
    boolean statusSmartcompr;
    double velocidadtest = MOTORSHOOTERLEFT.getSelectedSensorVelocity() / 4096 * 10 * 60 * 2;
    double x = tx.getDouble(0.0);
    double y = ty.getDouble(0.0);
    double area = ta.getDouble(0.0);
    double distancia_metros_limelight_a_hub = distanceFromLimelightToGoalInches * 2.54;
    
    capuchavalor = MOTORCAPUCHA.getSelectedSensorPosition();
    anguloFinal = -1*capuchavalor/ktick2Degree;
    //capucha_angulo =  capuchavalor/23400*360;


    // IMPRIME LOS VALORES EN EL SMARTDASHBOARD
    SmartDashboard.putNumber("distancia a HUB", distancia_metros_limelight_a_hub);
    SmartDashboard.putBoolean("Intake", !statusrobot.IntakeState);
    SmartDashboard.putBoolean("Compresor", !statusrobot.compresorState);
    SmartDashboard.putNumber("RPM shooter", velocidadesShooter.velocidad*-1);
    /*
     * SmartDashboard.putNumber("velocidad", velocidadtest);
     * SmartDashboard.putNumber("LL X Value", x);
     * SmartDashboard.putNumber("LL Y Value", y);
     * SmartDashboard.putNumber("LL X Area", area);
     */

    //SmartDashboard.putNumber("capucha angulo", capucha_angulo);
    SmartDashboard.putNumber("deegree", anguloFinal);
    SmartDashboard.putNumber("capucha", capuchavalor);
    SmartDashboard.putBoolean("Limit", limitcapucha.get());
    SmartDashboard.putNumber("Corriente Capucha", MOTORCAPUCHA.getSupplyCurrent());

  }

  @Override
  public void autonomousInit() {
    reiniciarSensores();
    desactivartodo();

  }

  @Override
  public void autonomousPeriodic() { // Autonomo
    AutonomoTaxi();
  }

  @Override
  public void teleopInit() {

    falconpidConfig();
    reiniciarSensores();
    desactivartodo();

  }

  @Override
  public void teleopPeriodic() { // Teleoperado

    // Mover Chassis
    double velocidad = JoystickDriver1.getRawAxis(Kxbox.AXES.RT) - JoystickDriver1.getRawAxis(Kxbox.AXES.LT);
    chasis.arcadeDrive(
        -VelocidadChasis.velocidadgiro * JoystickDriver1.getRawAxis(Kxbox.AXES.joystick_izquierdo_eje_X),
        -VelocidadChasis.velocidadX * -velocidad);

    // Intake
    compresorbotonB();
    IntakeBotA();
    returnHome();
    climbler();

    //Disparar
    if (JoystickDriver2.getRawAxis(Kxbox.AXES.LT) >= 0.5) {
      ShooterPID(velocidadesShooter.velocidad);
    } else {
      MOTORSHOOTERLEFT.set(0);
      MOTORSHOOTERRIGHT.set(0);
    }

    //Tiro Fender
    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.B)) {

      velocidadesShooter.velocidad = velocidadesShooter.fender;  //4650
      if(anguloFinal <= 8.5){
        MOTORCAPUCHA.set(0.3);
      }else{
        MOTORCAPUCHA.set(0);
      }
    }

    //Tarmac 1.84m
    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.A)) {

      velocidadesShooter.velocidad = velocidadesShooter.tarmac;
      if(anguloFinal <= 20){
        MOTORCAPUCHA.set(0.3);
      }else{
        MOTORCAPUCHA.set(0);
      }

    }

    //Launch Pad
    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.X)) {

      velocidadesShooter.velocidad = velocidadesShooter.launchpad;
      if(anguloFinal <= 25){
        MOTORCAPUCHA.set(0.3);
      }else{
        MOTORCAPUCHA.set(0);
      }

    }

    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.LB) == true) {

      MOTORCAPUCHA.setSelectedSensorPosition(0);

    }

    if (JoystickDriver2.getRawAxis(Kxbox.AXES.RT) >= 0.5) {

      MOTORINDEXER.set(0.5);
    } else {
      MOTORINDEXER.set(0);
    }


    if(JoystickDriver2.getRawButton(Kxbox.BOTONES.boton_con_cuadritos)){

      MOTORCAPUCHA.set(0.4 * -JoystickDriver2.getRawAxis(Kxbox.AXES.joystick_derecho_eje_Y));
    }
  }
  

  @Override
  public void disabledInit() {
    reiniciarSensores();
    desactivartodo();

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

  /*
   *
   *
   * // SEPARACION DE PERIODOS//
   *
   *
   */

  public void compresorbotonB() {

    // SE PRENDE EL COMPRESOR CON EL BOTON "B"
    // Mas adelante cambiar esto al driver secundario
    if (JoystickDriver1.getRawButtonPressed(ControlarMecanismos.compresor)) {
      if (statusrobot.compresorState) {
        COMPRESOR.enableDigital();
        statusrobot.compresorState = false;
      } else {
        COMPRESOR.disable();
        statusrobot.compresorState = true;
      }
    }
  }

  public void IntakeBotA() {

    if (JoystickDriver1.getRawButtonPressed(Kxbox.BOTONES.A)) {
      if (statusrobot.IntakeState) {
        PISTINTAKE.set(true);
        statusrobot.IntakeState = false;

        if (JoystickDriver1.getRawButton(Kxbox.BOTONES.Y)) {

          MOTORINTAKE.set(0.4);
  
        } else {
          MOTORINTAKE.set(-0.4);


      }
    }else {
        PISTINTAKE.set(false);
        MOTORINTAKE.set(-0);

        statusrobot.IntakeState = true;

      }

    }
  }

  public void desactivartodo() {

    // Desactiva totalmente todo, incluso si ya estaba desactivado antes
    chasis.arcadeDrive(0, 0);
    PISTCHASIS.set(Value.kOff);
    PISTINTAKE.set(false);
    MOTORINTAKE.set(0);
    MOTORINDEXER.set(0);
    MOTORSHOOTERLEFT.set(0);
    MOTORSHOOTERRIGHT.set(0);

  }

  public void reiniciarSensores() {

    // Reset de sensores de encoders, navx.
    MOTORD1ENC.setSelectedSensorPosition(0);
    MOTORI4ENC.setSelectedSensorPosition(0);
    navx.reset();
    COMPRESOR.disable();
    statusrobot.IntakeState = false;
    statusrobot.compresorState = false;

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

    if (distmeters <= 3) {
      chasis.arcadeDrive(0, 0.5);
    }

    if (distmeters >= 3 && distmeters <= 2.05) {
      chasis.arcadeDrive(0, 0);
    }

    if (distmeters >= 3.1) {
      chasis.arcadeDrive(0, -0.3);
    }

    double direccionx = navx.getDisplacementX();
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
    }

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

    MOTORSHOOTERLEFT.configOpenloopRamp(1.4);

    // https://phoenix-documentation.readthedocs.io/en/latest/ch14_MCSensor.html#

  }

  public void ShooterPID(double rpmtotal) { // 6380 maximo

    // https://phoenix-documentation.readthedocs.io/en/latest/ch14_MCSensor.html#
    double rpmconv = KPIDShooter.torpm * rpmtotal;
    double valor = -1 * rpmconv;// JoystickDriver1.getRawAxis(Kxbox.AXES.joystick_derecho_eje_Y);

    SmartDashboard.putNumber("conv", rpmconv);

    double targetVelocity_UnitsPer100ms = valor * 3000 * 2048.0 / 600.0;
    MOTORSHOOTERLEFT.set(TalonFXControlMode.Velocity, targetVelocity_UnitsPer100ms);
    _sb.setLength(0);
    MOTORSHOOTERRIGHT.set(TalonFXControlMode.Velocity, -targetVelocity_UnitsPer100ms);
  }

  public void ajustedegiro() { // Probar

    double x = tx.getDouble(0.0);
    double ajusteGiro = 0.0f;
    float min_command = 0.05f;

    if (x > 1.0) {

      ajusteGiro = Constants.LimeLight.kp * x - min_command;

    } else if (x < 1.0) {

      ajusteGiro = Constants.LimeLight.kp * x + min_command;

    }
    chasis.arcadeDrive(ajusteGiro, 0);

  }

  public void chasis_shoot_Adjust() { // Probar
    double x = tx.getDouble(0.0);
    double ajusteGiro = 0.0f;
    float min_aim_command = 0.05f;

    double heading_error = -tx.getDouble(0.0);
    double distance_error = -ty.getDouble(0.0);

    if (x > 1.0) {
      ajusteGiro = Constants.LimeLight.kp * heading_error * x - min_aim_command;
    } else if (x < 1.0) {
      ajusteGiro = Constants.LimeLight.kp * heading_error * x + min_aim_command;
    }
    double distance_adjust = Constants.LimeLight.kp * distance_error;
    chasis.arcadeDrive(ajusteGiro, distance_adjust);
  }

  public void climbler() { // Probar

  
    if (JoystickDriver2.getPOV() == Kxbox.POV.arriba) {

      MOTORCLIMBER.set(-1);
    }
    if (JoystickDriver2.getPOV() == Kxbox.POV.abajo) {

      MOTORCLIMBER.set(0.4);
    } else if (JoystickDriver2.getPOV() == -1) {
MOTORCLIMBER.set(0);
    }

  }

  public void returnHome () {

    if (JoystickDriver2.getRawButton(Kxbox.BOTONES.RB) ){
      if(limitcapucha.get() == true){
        MOTORCAPUCHA.set(-0.4);
      }else{
        MOTORCAPUCHA.set(0);
      }
    }    
  }

  public void resetLimitSwitch(){
    if(limitcapucha.get()==false){
      MOTORCAPUCHA.setSelectedSensorPosition(0);
    }
  }
}
