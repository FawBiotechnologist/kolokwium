package edu.iis.mto.oven;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.print.attribute.standard.OrientationRequested;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OvenTest {
    private static final int DEFAULT_STAGE_TIME = 10;
    private static final int DEFAULT_INITIAL_TEMP = 100;
    private static final int DEFAULT_TARGET_TEMP = 300;

    @Mock
    Fan fanMock;
    @Mock
    HeatingModule heatingModuleMock;

    Oven oven;
    ProgramStage someUnimportantProgramStageOfTypeThermoCirculation = ProgramStage.builder()
            .withStageTime(DEFAULT_STAGE_TIME)
            .withHeat(HeatType.THERMO_CIRCULATION)
            .withTargetTemp(DEFAULT_TARGET_TEMP)
            .build();
    ProgramStage someUnimportantProgramStageOfTypeGrill = ProgramStage.builder()
            .withStageTime(DEFAULT_STAGE_TIME)
            .withHeat(HeatType.GRILL)
            .withTargetTemp(DEFAULT_TARGET_TEMP)
            .build();
    ProgramStage someUnimportantProgramStageOfTypeHeater = ProgramStage.builder()
            .withStageTime(DEFAULT_STAGE_TIME)
            .withHeat(HeatType.HEATER)
            .withTargetTemp(DEFAULT_TARGET_TEMP)
            .build();
    BakingProgram defaultBakingProgramOfTypeGrillWithCoolAtFinish = BakingProgram.builder()
            .withInitialTemp(DEFAULT_INITIAL_TEMP)
            .withStages(List.of(someUnimportantProgramStageOfTypeGrill))
            .withCoolAtFinish(true)
            .build();
    BakingProgram defaultBakingProgramOfTypeThermoCirculationWithoutCoolAtFinish = BakingProgram.builder()
            .withInitialTemp(DEFAULT_INITIAL_TEMP)
            .withStages(List.of(someUnimportantProgramStageOfTypeThermoCirculation))
            .withCoolAtFinish(false)
            .build();
    BakingProgram defaultBakingProgramOfTypeHeaterWithoutCoolAtFinish = BakingProgram.builder()
            .withInitialTemp(DEFAULT_INITIAL_TEMP)
            .withStages(List.of(someUnimportantProgramStageOfTypeHeater))
            .withCoolAtFinish(false)
            .build();

    @BeforeEach
    void setUp() {
        oven = new Oven(heatingModuleMock, fanMock);
    }


    @Test
    void itCompiles() {
        assertThat(true, equalTo(true));
    }

    @Test
    void successfulGrillRunWithoutErrors() {
        Assertions.assertDoesNotThrow(() -> oven.runProgram(defaultBakingProgramOfTypeGrillWithCoolAtFinish));
    }

    @Test
    void successfulThermoCirculationRunWithoutErrors() {
        Assertions.assertDoesNotThrow(() -> oven.runProgram(defaultBakingProgramOfTypeThermoCirculationWithoutCoolAtFinish));
    }

    @Test
    void successfulHeaterRunWithoutErrors() {
        Assertions.assertDoesNotThrow(() -> oven.runProgram(defaultBakingProgramOfTypeHeaterWithoutCoolAtFinish));
    }

    @Test
    void damagedHeatingModuleExpectingOvenException() throws HeatingException {
        doThrow(HeatingException.class).when(heatingModuleMock).heater(any());
        Assertions.assertThrows(OvenException.class, () -> oven.runProgram(defaultBakingProgramOfTypeGrillWithCoolAtFinish));
    }

    @Test
    void someErrorInHeatingModuleOccurredDuringGrillingExpectingOvenException() throws HeatingException {
        doThrow(HeatingException.class).when(heatingModuleMock).grill(any());
        Assertions.assertThrows(OvenException.class, () -> oven.runProgram(defaultBakingProgramOfTypeGrillWithCoolAtFinish));
    }

    @Test
    void damagedFanExpectingRuntimeException() {
        doThrow(RuntimeException.class).when(fanMock).on();
        Assertions.assertThrows(RuntimeException.class, () -> oven.runProgram(defaultBakingProgramOfTypeThermoCirculationWithoutCoolAtFinish));
    }

    //Testy behavioralne
    @Test
    void successfulGrillRunWithoutErrorsCallsOrderCheck() throws HeatingException {
        InOrder order = Mockito.inOrder(fanMock, heatingModuleMock);
        oven.runProgram(defaultBakingProgramOfTypeGrillWithCoolAtFinish);
        order.verify(heatingModuleMock).heater(any());
        order.verify(fanMock).isOn();
        order.verify(heatingModuleMock).grill(any());
        order.verify(fanMock).on();
        order.verifyNoMoreInteractions();
    }

    @Test
    void successfulThermoCirculationRunWithoutErrorsCallsOrderCheck() throws HeatingException {
        InOrder order = Mockito.inOrder(fanMock, heatingModuleMock);
        oven.runProgram(defaultBakingProgramOfTypeThermoCirculationWithoutCoolAtFinish);
        order.verify(heatingModuleMock).heater(any());
        order.verify(fanMock).on();
        order.verify(heatingModuleMock).termalCircuit(any());
        order.verify(fanMock).off();
        order.verifyNoMoreInteractions();
    }

    @Test
    void successfulHeaterRunWithoutErrorsCallsOrderCheck() throws HeatingException {
        InOrder order = Mockito.inOrder(fanMock, heatingModuleMock);
        oven.runProgram(defaultBakingProgramOfTypeHeaterWithoutCoolAtFinish);
        order.verify(heatingModuleMock).heater(any());
        order.verify(fanMock).isOn();
        order.verify(heatingModuleMock).heater(any());
        order.verifyNoMoreInteractions();
    }

    @Test
    void testIfFanIGettingTurnedOffWhenItIsAskedTo() {
        when(fanMock.isOn()).thenReturn(true);
        InOrder order = Mockito.inOrder(fanMock);
        oven.runProgram(defaultBakingProgramOfTypeHeaterWithoutCoolAtFinish);
        order.verify(fanMock).off();
        order.verifyNoMoreInteractions();
    }


}
