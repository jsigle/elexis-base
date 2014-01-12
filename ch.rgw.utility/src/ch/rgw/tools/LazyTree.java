/*******************************************************************************
 * Copyright (c) 2005-2008, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.rgw.tools;

import java.util.Collection;
import java.util.Comparator;

/**
 * Ein Tree, der seine Children erst bei Bedarf lädt. Dazu muss ein LazyTreeListener übergeben
 * werden, der die Children liefern muss.
 * 
 * @author gerry
 * 
 */
public class LazyTree<T> extends Tree<T> {
	LazyTreeListener listen;
	
	public LazyTree(Tree<T> p, T elem, LazyTreeListener l, Comparator<T> comp){
		super(p, elem, comp);
//System.out.println("js: LazyTree: LazyTree(p, elem, l, comp) has begun, now just after super(p, elem, comp)");
//System.out.println("js: LazyTree: LazyTree(p, elem, l, comp) about to listen=l");
		listen = l;
//System.out.println("js: LazyTree: LazyTree(p, elem, l, comp) end");
	}
	
	public LazyTree(Tree<T> p, T elem, LazyTreeListener l){
		super(p, elem);
//System.out.println("js: LazyTree: LazyTree(p, elem, l) has begun, now just after super(p, elem)");
//System.out.println("js: LazyTree: LazyTree(p, elem, l) about to listen=l");
		listen = l;
//System.out.println("js: LazyTree: LazyTree(p, elem, l) end");
	}
	
	public LazyTree(Tree<T> p, T elem, IFilter f, LazyTreeListener l){
		super(p, elem, f);
//System.out.println("js: LazyTree: LazyTree(p, elem, f, l) has begun, now just after super(p, elem, f)");
//System.out.println("js: LazyTree: LazyTree(p, elem, f, l) about to listen=l");
		listen = l;
//System.out.println("js: LazyTree: LazyTree(p, elem, f, l) end");
	}
	
	public Collection<Tree<T>> getChildren(){
//System.out.println("js: LazyTree: getChildren() begin");
//System.out.println("js: LazyTree: getChildren() about to loadChildren()");
		loadChildren();
//System.out.println("js: LazyTree: getChildren() end - about to return super.getChildren()");
		return super.getChildren();
	}
	
	public boolean hasChildren(){
//System.out.println("js: LazyTree: hasChildren() begin");
		if (first == null) {
//System.out.println("js: LazyTree: hasChildren() end - about to return (listen == null? false : listen.hasChildren(this))");
			return (listen == null ? false : listen.hasChildren(this));
		}
//System.out.println("js: LazyTree: hasChildren() end - about to return true");
		return true;
	}
	
	public LazyTree<T> add(T elem, LazyTreeListener l){
//System.out.println("js: LazyTree: add(T, l) begin");
		LazyTree<T> ret = new LazyTree<T>(this, elem, filter, l);
//System.out.println("js: LazyTree: add(T, l) end - returning ret = new LazyTree<T>(this, elem, filter, l)");
		return ret;
	}
	
	// Stack Overflow?? //TODO
	private void loadChildren(){
//System.out.println("js: LazyTree: loadChildren() begin");
		if ((first == null) && (listen != null)) {
//System.out.println("js: LazyTree: loadChildren() about to listen.fetchChildren(this)");
			listen.fetchChildren(this);
		}
//System.out.println("js: LazyTree: loadChildren() end");
	}
	
	public Tree<T> getFirstChild(){
//System.out.println("js: LazyTree: getFirstChild() begin");
//System.out.println("js: LazyTree: getFirstChild() about to loadChildren()");
		loadChildren();
//System.out.println("js: LazyTree: getFirstChild() end - aout to return first");
		return first;
	}
	
	public interface LazyTreeListener {
		/**
		 * fetch children of this node.
		 * 
		 * @param l
		 * @return true if children were added
		 */
		public boolean fetchChildren(LazyTree<?> l);
		
		/**
		 * return true if this node has children
		 * 
		 * @param l
		 * @return
		 */
		public boolean hasChildren(LazyTree<?> l);
	}
	
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	@Override
	public synchronized Tree move(Tree newParent){
//System.out.println("js: LazyTree: move() begin");
		if (!(newParent instanceof LazyTree)) {
			preload();
			
		}
//System.out.println("js: LazyTree: move() end - returning super.move(newParent)");
		return super.move(newParent);
	}
	
	public Tree preload(){
//System.out.println("js: LazyTree: preload() begin");
		loadChildren();
		for (Tree child = first; child != null; child = child.next) {
			if (child instanceof LazyTree) {
				((LazyTree) child).preload();
			}
		}
//System.out.println("js: LazyTree: preload() end - returning this");
		return this;
	}
	
}
