package pipe.actions.file;

import org.junit.Before;
import org.junit.Test;
import pipe.actions.gui.ExportPNGAction;
import pipe.views.PipeApplicationView;
import pipe.controllers.application.PipeApplicationController;

import javax.swing.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ExportPNGActionTest {
    ExportPNGAction exportPNGAction;
    PipeApplicationView mockView;
    PipeApplicationController mockController;

    @Before
    public void setUp()
    {
        mockView = mock(PipeApplicationView.class);
        mockController = mock(PipeApplicationController.class);
        exportPNGAction = new ExportPNGAction(mockController, mockView);
    }

    @Test
    public void actionPerformed()
    {
        //TODO: Needs a re-write to be able to test static
    }

    @Test
    public void setShortDescription()
    {
        Object shortDescription = exportPNGAction.getValue(Action.SHORT_DESCRIPTION);
        assertEquals("Export the net to PNG format", shortDescription);
    }

    @Test
    public void setKeyboardShortcut()
    {
        Object acceleratorKey = exportPNGAction.getValue(Action.ACCELERATOR_KEY);
        KeyStroke stroke = KeyStroke.getKeyStroke("meta G");
        assertEquals(stroke, acceleratorKey);
    }
}
