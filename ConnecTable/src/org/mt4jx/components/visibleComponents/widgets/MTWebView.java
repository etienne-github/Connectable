package org.mt4jx.components.visibleComponents.widgets;

import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.flickProcessor.FlickEvent;
import org.mt4j.input.inputProcessors.componentProcessors.flickProcessor.FlickProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.rotateProcessor.RotateProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.scaleProcessor.ScaleEvent;
import org.mt4j.input.inputProcessors.componentProcessors.scaleProcessor.ScaleProcessor;
import org.mt4j.util.MTColor;
import org.mt4j.util.PlatformUtil;
import org.mt4j.util.math.Plane;
import org.mt4j.util.math.Ray;
import org.mt4j.util.math.Tools3D;
import org.mt4j.util.math.Vector3D;
import org.mt4j.util.opengl.GL10;
import org.mt4j.util.opengl.GLTexture;

import processing.core.PApplet;

import com.badlogic.gdx.awesomium.JSArguments;
import com.badlogic.gdx.awesomium.JSValue;
import com.badlogic.gdx.awesomium.MouseButton;
import com.badlogic.gdx.awesomium.Rect;
import com.badlogic.gdx.awesomium.RenderBuffer;
import com.badlogic.gdx.awesomium.URLFilteringMode;
import com.badlogic.gdx.awesomium.WebCore;
import com.badlogic.gdx.awesomium.WebView;
import com.badlogic.gdx.awesomium.WebViewListener;

public class MTWebView extends MTRectangle{
	private static WebCore webCore;
	private static int instances = 0;
	private int zoom = 100;
	private WebView webView;
	private GLTexture texture;
	private int browserWidth;
	private int browserHeight;

	public MTWebView(PApplet pApplet, int width, int height) {
		super(pApplet, width, height);
		
		this.browserWidth = width;
		this.browserHeight = height;
		
		if (webCore == null){
			webCore = new WebCore(".");	
		}
		
		this.webView = webCore.createWebView(width, height);
		this.webView.focus();
		this.webView.loadURL("http://www.google.fr", "", "", "");
		instances += 1;
		
		texture = new GLTexture(pApplet, width, height);
		this.setTexture(texture);
		
		this.setStrokeColor(MTColor.BLACK);
		
		//So gestures dont stop if the cursor isnt on the browser anymore
		final Plane hitTestPlane = new Plane(new Vector3D(this.getVerticesLocal()[0]), new Vector3D(0,0,1));
		
		this.removeAllGestureEventListeners(DragProcessor.class);
		this.addGestureListener(DragProcessor.class, new IGestureEventListener() {
			@Override
			public boolean processGestureEvent(MTGestureEvent ge) {
				DragEvent de = (DragEvent)ge;
				
				Ray ray = Tools3D.getCameraPickRay(getRenderer(), MTWebView.this, de.getDragCursor());
				Ray localRay = MTWebView.this.globalToLocal(ray);
				Vector3D intersection = hitTestPlane.getIntersectionLocal(localRay);
				if (intersection != null){
					switch (de.getId()) {
					case MTGestureEvent.GESTURE_STARTED:
						webView.injectMouseMove((int)intersection.x, (int)intersection.y);
						webView.injectMouseDown(MouseButton.Left);
						break;

					case MTGestureEvent.GESTURE_UPDATED:
						webView.injectMouseMove((int)intersection.x, (int)intersection.y);
						break;
					case MTGestureEvent.GESTURE_ENDED:
					case MTGestureEvent.GESTURE_CANCELED:
						webView.injectMouseMove((int)intersection.x, (int)intersection.y);
						webView.injectMouseUp(MouseButton.Left);
						break;
					default:
						break;
					}
				}
				return false;
			}
		});
		
		//Scroll and scale using scale processor
		this.removeAllGestureEventListeners(RotateProcessor.class);
		this.removeAllGestureEventListeners(ScaleProcessor.class);
		this.addGestureListener(ScaleProcessor.class, new ScrollListener());
		this.addGestureListener(ScaleProcessor.class, new ZoomListener());
		
		//Flick through history
		this.registerInputProcessor(new FlickProcessor(300, 9));
		this.addGestureListener(FlickProcessor.class, new IGestureEventListener() {
			@Override
			public boolean processGestureEvent(MTGestureEvent ge) {
				FlickEvent fe = (FlickEvent)ge;
				if (fe.getId() == MTGestureEvent.GESTURE_ENDED && fe.isFlick()){
					switch (fe.getDirection()) {
					case WEST:
					case NORTH_WEST:
					case SOUTH_WEST:
						getWebView().goToHistoryOffset(-1);
						break;
					case EAST:
					case NORTH_EAST:
					case SOUTH_EAST:
						getWebView().goToHistoryOffset(1);
						break;
					default:
						break;
					}
				}
				return false;
			}
		});
		
		/*
		this.registerInputProcessor(new TapProcessor(getMTApplication()));
		this.addGestureListener(TapProcessor.class, new IGestureEventListener() {
			@Override
			public boolean processGestureEvent(MTGestureEvent ge) {
				TapEvent te = (TapEvent)ge;
				
				Ray ray = Tools3D.getCameraPickRay(getRenderer(), MTBrowser.this, te.getCursor());
				Ray localRay = MTBrowser.this.globalToLocal(ray);
				
				Vector3D intersection = MTBrowser.this.getIntersectionLocal(localRay);
				if (intersection != null){
					switch (te.getTapID()) {
					case TapEvent.TAP_DOWN:
						webView.injectMouseMove((int)intersection.x, (int)intersection.y);
						webView.injectMouseDown(MouseButton.Left);
						break;
					case TapEvent.TAP_UP:
						webView.injectMouseMove((int)point.x, (int)point.y);
						webView.injectMouseUp(MouseButton.Left);
						break;
					case TapEvent.TAPPED:
						
						break;
					default:
						break;
					}
				}
				
				
				return false;
			}
		});
		*/
		
		resetZoom();
		webView.setTransparent(true);
		webView.setListener(new WebViewListener() {
			@Override
			public void onWebViewCrashed() {	}
			
			@Override
			public void onRequestMove(int x, int y) {
				System.out.println("onRequestMove - x: " + x + " y: " + y);
			}
			
			@Override
			public void onRequestDownload(String url) {
				System.out.println("onRequestDownload  - url: " + url);
			}
			
			@Override
			public void onReceiveTitle(String title, String frameName) {	}
			
			@Override
			public void onPluginCrashed(String pluginName) {	}
			
			@Override
			public void onOpenExternalLink(String url, String source) {
				System.out.println("onOpenExternalLink - url: " + url + " source: " + source);
				loadURL(url, "", "", "");
			}
			
			@Override
			public void onGetPageContents(String url, String contents) {
				System.out.println("onGetPageContents - url: " + url + " contents: " + contents);
			} 
			
			@Override
			public void onFinishLoading() {		}
			
			@Override
			public void onDOMReady() {		}
			
			@Override
			public void onChangeTooltip(String tooltip) {
				System.out.println("onChangeTooltip - tooltip: " + tooltip);
			}
			
			@Override
			public void onChangeTargetURL(String url) {
				System.out.println("onChangeTargetURL - url: " + url);
			}
			
			@Override
			public void onChangeKeyboardFocus(boolean isFocused) {
				System.out.println("onChangeKeyboardFocus - isFocused: " + isFocused);
			}
			
			@Override
			public void onChangeCursor(int cursor) {
				System.out.println("onChangeCursor");
			}
			
			@Override
			public void onCallback(String objectName, String callbackName, JSArguments args) {
				System.out.println("onCallback, objectName: " + objectName + " callbackname: " + callbackName + " args: " + args);
			}
			
			@Override
			public void onBeginNavigation(String url, String frameName) {
				System.out.println("onBeginNavigation - url: " + url + " frameName: " + frameName);
			} 
			
			@Override
			public void onBeginLoading(String url, String frameName, int statusCode, String mimeType) {		}
		});
	}
	
	

	@Override
	protected void destroyComponent() {
		super.destroyComponent();
		webView.destroy();
//		webCore.dispose();
		instances -=1;
		if (instances <= 0){
			webCore.dispose();
			webCore = null;
		}
	}
	
	
	@Override
	public void updateComponent(long timeDelta) {
		super.updateComponent(timeDelta);
		webCore.update();
		if (webView.isDirty()) {
			RenderBuffer renderBuffer = webView.render();
			if (renderBuffer != null) {
//				GL gl = Tools3D.getGL(getRenderer());
				GL10 gl = PlatformUtil.getGL();
				gl.glBindTexture(texture.getTextureTarget(), texture.getTextureID());
				gl.glTexImage2D(texture.getTextureTarget(), 0, GL10.GL_RGBA, this.browserWidth, this.browserHeight, 0, 0x80E1, GL10.GL_UNSIGNED_BYTE,
					renderBuffer.getBuffer());
				
//				texture.updateGLTexture(renderBuffer.getBuffer());
			}
		}
	}
	
	public WebView getWebView(){
		return this.webView;
	}
	
	
	private class ScrollListener implements IGestureEventListener{
		private Vector3D lastMiddle;
		public boolean processGestureEvent(MTGestureEvent g) {
			if (g instanceof ScaleEvent){
				ScaleEvent se = (ScaleEvent)g;
				//Add a little panning to scale, so if we can pan while we scale
				InputCursor c1 = se.getFirstCursor();
				InputCursor c2 = se.getSecondCursor();
				if (se.getId() == MTGestureEvent.GESTURE_STARTED){
					Vector3D i1 = c1.getPosition();
					Vector3D i2 = c2.getPosition();
					lastMiddle = i1.getAdded(i2.getSubtracted(i1).scaleLocal(0.5f));
				}else if (se.getId() == MTGestureEvent.GESTURE_UPDATED){ 
					Vector3D i1 =  c1.getPosition();
					Vector3D i2 =  c2.getPosition();
					Vector3D middle = i1.getAdded(i2.getSubtracted(i1).scaleLocal(0.5f));
					Vector3D middleDiff = middle.getSubtracted(lastMiddle);
					getWebView().injectMouseWheel(Math.round(middleDiff.y*2));
					lastMiddle = middle;
				}
			}
			return false;
		}
	}
	
	
	private class ZoomListener implements IGestureEventListener{
		private float lastDist;
		private float scaleThreshhold = 55;
		private int zoomAmount = 10;
		public boolean processGestureEvent(MTGestureEvent g) {
			if (g instanceof ScaleEvent){
				ScaleEvent se = (ScaleEvent)g;
				if (se.getId() == MTGestureEvent.GESTURE_STARTED){
					lastDist = se.getFirstCursor().getPosition().distance2D(se.getSecondCursor().getPosition());
					//System.out.println("lastDist: " + lastDist);
				}else if (se.getId() == MTGestureEvent.GESTURE_UPDATED){ 
					float currentDist = se.getFirstCursor().getPosition().distance2D(se.getSecondCursor().getPosition());
					float diff = currentDist-lastDist;
					//System.out.println("CurrentDist: " + currentDist + " lastdist: " + lastDist + " Diff: " + diff);
					if(diff > 0 && Math.abs(diff) > scaleThreshhold){
						setZoom(getZoom()+zoomAmount);
						lastDist = currentDist;
					}else if(diff < 0 && Math.abs(diff) > scaleThreshhold){
						setZoom(getZoom()-zoomAmount);
						lastDist = currentDist;
					}
				}
			}
			return false;
		}
	}
	
	
	
	private int getZoom(){
		return zoom;
	}
	
	public void setZoom(int percent){
		this.zoom = percent;
		this.getWebView().setZoom(percent);
	}
	
	
	public void resetZoom(){
		this.zoom = 100;
		this.getWebView().resetZoom();
	}
	
	
	
	public void loadURL(String url, String frameName, String username, String password) {
		webView.loadURL(url, frameName, username, password);
	}

	public void loadHTML(String html, String frameName) {
		webView.loadHTML(html, frameName);
	}

	public void loadFile(String file, String frameName) {
		webView.loadFile(file, frameName);
	}
	
	
	public void addURLFilter(String filter) {
		webView.addURLFilter(filter);
	}

	public void callJavascriptFunction(String object, String function,
			JSArguments args, String frameName) {
		webView.callJavascriptFunction(object, function, args, frameName);
	}

	public void clearAllURLFilters() {
		webView.clearAllURLFilters();
	}

	public void copy() {
		webView.copy();
	}

	public void createObject(String objectName) {
		webView.createObject(objectName);
	}

	public void cut() {
		webView.cut();
	}

	public void destroyObject(String objectName) {
		webView.destroyObject(objectName);
	}

	public void executeJavascript(String javascript, String frameName) {
		webView.executeJavascript(javascript, frameName);
	}

	public JSValue executeJavascriptWithResult(String javascript,
			String frameName, int timeoutMS) {
		return webView.executeJavascriptWithResult(javascript, frameName,
				timeoutMS);
	}

	public JSValue executeJavascriptWithResult(String javascript,
			String frameName) {
		return webView.executeJavascriptWithResult(javascript, frameName);
	}

	public void focus() {
		webView.focus();
	}

	public Rect getDirtyBounds() {
		return webView.getDirtyBounds();
	}

	public void goToHistoryOffset(int offset) {
		webView.goToHistoryOffset(offset);
	}

	public boolean isDirty() {
		return webView.isDirty();
	}

	public boolean isLoadingPage() {
		return webView.isLoadingPage();
	}

	public boolean isResizing() {
		return webView.isResizing();
	}

	public void paste() {
		webView.paste();
	}

	public void pauseRendering() {
		webView.pauseRendering();
	}

	public void reload() {
		webView.reload();
	}

	public RenderBuffer render() {
		return webView.render();
	}

	public void resize(int width, int height, boolean waitForRepaint,
			int repaintTimeoutMS) {
		webView.resize(width, height, waitForRepaint, repaintTimeoutMS);
	}

	public void resumeRendering() {
		webView.resumeRendering();
	}

	public void setListener(WebViewListener listener) {
		webView.setListener(listener);
	}

	public void setObjectCallback(String objectName, String callbackName) {
		webView.setObjectCallback(objectName, callbackName);
	}

	public void setObjectProperty(String objectName, String propName,
			JSValue value) {
		webView.setObjectProperty(objectName, propName, value);
	}

	public void setTransparent(boolean isTransparent) {
		webView.setTransparent(isTransparent);
	}

	public void setURLFilteringMode(URLFilteringMode mode) {
		webView.setURLFilteringMode(mode);
	}

	public void stop() {
		webView.stop();
	}

	public void unfocus() {
		webView.unfocus();
	}



	public void injectKeyDown(int virtualKeyCode, int modifiers,
			boolean isSystemKey) {
		webView.injectKeyDown(virtualKeyCode, modifiers, isSystemKey);
	}



	public void injectKeyTyped(char character) {
		webView.injectKeyTyped(character);
	}



	public void injectKeyUp(int virtualKeyCode, int modifiers,
			boolean isSystemKey) {
		webView.injectKeyUp(virtualKeyCode, modifiers, isSystemKey);
	}



	public void injectMouseDown(MouseButton button) {
		webView.injectMouseDown(button);
	}



	public void injectMouseMove(int x, int y) {
		webView.injectMouseMove(x, y);
	}



	public void injectMouseUp(MouseButton button) {
		webView.injectMouseUp(button);
	}



	public void injectMouseWheel(int scrollAmount) {
		webView.injectMouseWheel(scrollAmount);
	}



	public void selectAll() {
		webView.selectAll();
	}
	
	
	
	
}
