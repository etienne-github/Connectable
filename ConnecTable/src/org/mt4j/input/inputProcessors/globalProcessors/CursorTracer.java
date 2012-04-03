/***********************************************************************
 * mt4j Copyright (c) 2008 - 2009 Christopher Ruff, Fraunhofer-Gesellschaft All rights reserved.
 *  
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************************************************/
package org.mt4j.input.inputProcessors.globalProcessors;

import java.util.HashMap;
import java.util.Map;

import org.mt4j.AbstractMTApplication;
import org.mt4j.components.MTComponent;
import org.mt4j.components.bounds.IBoundingShape;
import org.mt4j.components.visibleComponents.shapes.AbstractShape;
import org.mt4j.components.visibleComponents.shapes.MTEllipse;
import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.components.visibleComponents.widgets.MTOverlayContainer;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.sceneManagement.Iscene;
import org.mt4j.util.MT4jSettings;
import org.mt4j.util.PlatformUtil;
import org.mt4j.util.math.Vector3D;
import org.mt4j.util.math.Vertex;

import processing.core.PApplet;
import processing.core.PImage;

public class CursorTracer extends AbstractGlobalInputProcessor{
	
	/** The app. */
	private AbstractMTApplication app;
	
	/** The cursor id to display shape. */
	private Map<InputCursor, AbstractShape>cursorIDToDisplayShape;
	
	/** The scene. */
	private Iscene scene;
	
	/** The overlay group. */
	private MTComponent overlayGroup;
	
	private float ellipseRadius = 15;


	/**
	 * Instantiates a new cursor tracer.
	 * 
	 * @param mtApp the mt app
	 * @param currentScene the current scene
	 */
	public CursorTracer(AbstractMTApplication mtApp, Iscene currentScene){
		this.app = mtApp;
		this.scene = currentScene;
		this.cursorIDToDisplayShape = new HashMap<InputCursor, AbstractShape>();
		
		if (PlatformUtil.isAndroid()){
			ellipseRadius = 30;
		}
			
		this.overlayGroup = new MTOverlayContainer(app, "Cursor Trace group");
		mtApp.invokeLater(new Runnable() {
			public void run() {
				scene.getCanvas().addChild(overlayGroup);
			}
		});
		
	}

	/**
	 * Creates the display component.
	 * 
	 * @param applet the applet
	 * @param position the position
	 * 
	 * @return the abstract shape
	 */
	protected AbstractShape createDisplayComponent(PApplet applet, Vector3D position){
		MTRectangle displayShape = new MTRectangle(new Vertex(0,0,0), 20, 20, applet);
		displayShape.setPickable(false);
		displayShape.setDrawSmooth(true);
		displayShape.setNoStroke(true);
		PImage touchImage = app.loadImage(MT4jSettings.getInstance().getDefaultImagesPath() +
		"digit.png");
		displayShape.setTexture(touchImage);
		return displayShape;
	}
	
	/* (non-Javadoc)
	 * @see org.mt4j.input.inputProcessors.globalProcessors.AbstractGlobalInputProcessor#processInputEvtImpl(org.mt4j.input.inputData.MTInputEvent)
	 */
	@Override
	public void processInputEvtImpl(MTInputEvent inputEvent) {
		if (inputEvent instanceof AbstractCursorInputEvt) {
			AbstractCursorInputEvt cursorEvt = (AbstractCursorInputEvt)inputEvent;
			InputCursor c = ((AbstractCursorInputEvt)inputEvent).getCursor();
			Vector3D position = new Vector3D(cursorEvt.getX(), cursorEvt.getY());

			AbstractShape displayShape = null;
			switch (cursorEvt.getId()) {
			case AbstractCursorInputEvt.INPUT_STARTED:
				displayShape = createDisplayComponent(app, position);
				cursorIDToDisplayShape.put(c, displayShape);
				overlayGroup.addChild(displayShape);
				displayShape.setPositionGlobal(position);
				
//				compToCreationTime.put(displayShape, System.currentTimeMillis()); //FIXME REMOVE
//				displayShape.setUserData("Cursor", c);//FIXME REMOVE
				break;
			case AbstractCursorInputEvt.INPUT_UPDATED:
				displayShape = cursorIDToDisplayShape.get(c);
				if (displayShape != null){
					displayShape.setPositionGlobal(position);
				}
				break;
			case AbstractCursorInputEvt.INPUT_ENDED:
				displayShape = cursorIDToDisplayShape.remove(c);
				if (displayShape != null){
					displayShape.destroy();
				}
				break;
			default:
				break;
			}
		}
	}
}
