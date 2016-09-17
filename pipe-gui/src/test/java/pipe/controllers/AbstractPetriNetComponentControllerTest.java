package pipe.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pipe.historyActions.MultipleEdit;
import pipe.utilities.transformers.Contains;
import uk.ac.imperial.pipe.models.petrinet.Place;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.AbstractUndoableEdit;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AbstractPetriNetComponentControllerTest {

    DummyController controller;
    
    final List<String> eventQueue = new ArrayList<>();

    @Mock
    Place place;

    @Mock
    UndoableEditListener listener;

    @Mock
    UndoableEdit undoableEdit1;

    @Mock
    UndoableEdit undoableEdit2;

    @Before
    public void setUp() {
        controller = new DummyController(place, listener);
        eventQueue.clear();
    }

    @Test
    public void registeringEventCallsListener() {
        controller.addEdit(undoableEdit1);
        verify(listener).undoableEditHappened(argThat(Contains.thisAction(undoableEdit1)));
    }

    @Test
    public void multipleEditDoesNotCallListenerBeforeFinishing() {
        controller.startMultipleEdits();
        controller.addEdit(undoableEdit1);
        verify(listener, never()).undoableEditHappened(any(UndoableEditEvent.class));
    }

    @Test
    public void multipleEditCallsListenerOnFinish() {

        controller.startMultipleEdits();
        controller.addEdit(undoableEdit1);
        controller.finishMultipleEdits();

        UndoableEdit multipleEdit = new MultipleEdit(Arrays.asList(undoableEdit1));
        verify(listener).undoableEditHappened(argThat(Contains.thisAction(multipleEdit)));
    }

    @Test
    public void multipleEditCallsListenerOnFinishWithMultipleItems() {

        controller.startMultipleEdits();
        controller.addEdit(undoableEdit1);
        controller.addEdit(undoableEdit2);
        controller.finishMultipleEdits();

        UndoableEdit multipleEdit = new MultipleEdit(Arrays.asList(undoableEdit1, undoableEdit2));
        verify(listener).undoableEditHappened(argThat(Contains.thisAction(multipleEdit)));
    }

    @Test
    public void wontCallListenerOnFinishIfNoEventAdded() {
        controller.startMultipleEdits();
        controller.finishMultipleEdits();
        verify(listener, never()).undoableEditHappened(any(UndoableEditEvent.class));
    }
    
    @Test
    public void editsAreUndoneInReverseOrder() {
    	UndoableEdit dummyEdit1 = new DummyUndoableEdit("1");
    	UndoableEdit dummyEdit2 = new DummyUndoableEdit("2");    	
    	UndoableEdit multipleEdit = new MultipleEdit(Arrays.asList(dummyEdit1, dummyEdit2));
    	
    	multipleEdit.undo();
    	
    	assertEquals("undo-2", eventQueue.get(0));
    	assertEquals("undo-1", eventQueue.get(1));
    	assertEquals(2, eventQueue.size());
    }
    
    @Test
    public void editsAreRedoneInNormalOrder() {
    	UndoableEdit dummyEdit1 = new DummyUndoableEdit("1");
    	UndoableEdit dummyEdit2 = new DummyUndoableEdit("2");    	
    	UndoableEdit multipleEdit = new MultipleEdit(Arrays.asList(dummyEdit1, dummyEdit2));
    	
    	multipleEdit.undo();
    	eventQueue.clear();
    	multipleEdit.redo();
    	
    	assertEquals("redo-1", eventQueue.get(0));
    	assertEquals("redo-2", eventQueue.get(1));
    	assertEquals(2, eventQueue.size());
    }    

    public class DummyController extends AbstractPetriNetComponentController<Place> {

        protected DummyController(Place component, UndoableEditListener listener) {
            super(component, listener);
        }

        public void addEdit(UndoableEdit edit) {
            registerUndoableEdit(edit);
        }
    }
    
    public class DummyUndoableEdit extends AbstractUndoableEdit implements UndoableEdit {
    	String name;
    	
    	protected DummyUndoableEdit(String name) {
    		super();
    		this.name = name;
    	}
    	
    	@Override
    	public void undo() {
    		eventQueue.add("undo-" + name);
    	}
    	
    	@Override
    	public void redo() {
    		eventQueue.add("redo-" + name);
    	}
    }
}
