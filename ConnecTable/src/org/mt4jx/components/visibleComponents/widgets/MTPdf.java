package org.mt4jx.components.visibleComponents.widgets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.mt4j.AbstractMTApplication;
import org.mt4j.components.TransformSpace;
import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.components.visibleComponents.widgets.buttons.MTImageButton;
import org.mt4jx.components.visibleComponents.widgets.pdf.MTPDF;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragProcessor;
//import org.mt4j.input.inputProcessors.componentProcessors.lassoProcessor.IdragClusterable;
import org.mt4j.input.inputProcessors.componentProcessors.scaleProcessor.ScaleEvent;
import org.mt4j.input.inputProcessors.componentProcessors.scaleProcessor.ScaleProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapEvent;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapProcessor;
import org.mt4j.util.MT4jSettings;
import org.mt4j.util.MTColor;
import org.mt4j.util.math.Vector3D;
import processing.core.PApplet;
import processing.core.PImage;



public class MTPdf extends MTRectangle {
	
	/** The selected. */
	private boolean selected;
	
	
	private MTRectangle image;
	private int currentPage = 1;
    private ArrayList<PImage> pages;
    private PApplet pApplet;
    private PImage nextImage;
    private PImage previousImage;
    private MTImageButton nextButton;
    private MTImageButton previousButton;
    private float sizeX;
    private float sizeY;
    private MTPDF pdf;
    
    private static float pdfSizeX = 420;
	private static float pdfSizeY = 540;
	
    private static float topBarHeight = 20;
	
	private static float sideBarWidth = 7;
	
	private static float bottomBarHeight = 7;
	
	public MTPdf(String pdfLink, final PApplet pApplet) throws IOException{
		super(-sideBarWidth, -topBarHeight, pdfSizeX + 2*sideBarWidth , pdfSizeY + topBarHeight + bottomBarHeight , pApplet);
		this.setVisible(false);
		this.pApplet = pApplet;
		File file = new File(pdfLink);
		pdf = new MTPDF(pApplet, file);
		if(pdf.getWidthXY(TransformSpace.RELATIVE_TO_PARENT) > pdf.getHeightXY(TransformSpace.RELATIVE_TO_PARENT)){
			pdfSizeX = 420;
			pdfSizeY = pdf.getHeightXY(TransformSpace.RELATIVE_TO_PARENT) / (pdf.getWidthXY(TransformSpace.RELATIVE_TO_PARENT)/420);
		}
		else{
			pdfSizeY = 540;
			pdfSizeX = pdf.getWidthXY(TransformSpace.RELATIVE_TO_PARENT) / (pdf.getHeightXY(TransformSpace.RELATIVE_TO_PARENT)/540);
		
		}
//		pdfSizeX = pdf.getWidthXY(TransformSpace.RELATIVE_TO_PARENT)/3;
//		pdfSizeY =  pdf.getHeightXY(TransformSpace.RELATIVE_TO_PARENT)/3;
		
		
		this.sizeX = pdfSizeX + 2*sideBarWidth;
		this.sizeY = pdfSizeY + topBarHeight + bottomBarHeight;
		this.setSizeLocal(sizeX, sizeY);
		this.setStrokeColor(new MTColor(0,0,0));
		this.setFillColor(new MTColor(50,50,50,200));
        
        
        
        pdf.setSizeLocal(pdfSizeX, pdfSizeY);
        pdf.setAutoUpdate(true);
        pdf.setPickable(false);
		this.addChild(pdf);
		
		this.addGestureListener(DragProcessor.class, new IGestureEventListener() {
			@Override
			public boolean processGestureEvent(MTGestureEvent ge) {
				if(ge instanceof DragEvent && pdf.isAutoUpdate()){
					switch (ge.getId()) {
					case ScaleEvent.GESTURE_ENDED:
						((AbstractMTApplication) pApplet).invokeLater(
								new Thread(){
								public void run(){
									pdf.updateTexture();
								}
							}
						);
						pdf.updateTexture();
						break;
					default:
						break;
					}
				}
				return false;
			}
		});
		
		this.addGestureListener(ScaleProcessor.class, new IGestureEventListener() {
			@Override
			public boolean processGestureEvent(MTGestureEvent ge) {
				if(ge instanceof ScaleEvent && pdf.isAutoUpdate()){
					switch (ge.getId()) {
					case ScaleEvent.GESTURE_ENDED:
						((AbstractMTApplication) pApplet).invokeLater(
								new Thread(){
								public void run(){
									pdf.updateTexture();
								}
							}
						);
						pdf.updateTexture();
						break;
					default:
						break;
					}
				}
				return false;
			}
		});
		
		//Draw this component and its children above 
		//everything previously drawn and avoid z-fighting
		this.setDepthBufferDisabled(true);
		
		nextImage = pApplet.loadImage(MT4jSettings.getInstance().getDefaultImagesPath() + "nextButton64.png");
		nextButton = new MTImageButton(nextImage, pApplet);
		
		nextButton.addGestureListener(TapProcessor.class, new IGestureEventListener() {
			public boolean processGestureEvent(MTGestureEvent ge) {
				TapEvent te = (TapEvent)ge;
				if (te.isTapped()){
					nextPage();
				}
				return true;
			}
		});
		
		this.addChild(nextButton);
		nextButton.setNoStroke(true);
		nextButton.setSizeLocal(30, 30);
		nextButton.setPositionRelativeToParent(new Vector3D(this.sizeX - 20, -10));
		
		previousImage = pApplet.loadImage(MT4jSettings.getInstance().getDefaultImagesPath() + "previousButton64.png");
		previousButton = new MTImageButton(previousImage, pApplet);
		
		previousButton.addGestureListener(TapProcessor.class, new IGestureEventListener() {
			public boolean processGestureEvent(MTGestureEvent ge) {
				TapEvent te = (TapEvent)ge;
				if (te.isTapped()){
					previousPage();
				}
				return true;
			}
		});
		
		this.addChild(previousButton);
		previousButton.setNoStroke(true);
		previousButton.setSizeLocal(30, 30);
		previousButton.setPositionRelativeToParent(new Vector3D(5, -10));
		
		this.setVisible(true);
	}
	
	public void nextPage(){
		if(currentPage < pdf.getNumberOfPages())
			pdf.setPageNumber(++currentPage);
	}
	
	public void previousPage(){
		if(currentPage > 1)
			pdf.setPageNumber(--currentPage);
	}
	
	public void firstPage(){
		pdf.setPageNumber(0);
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}
