/*******************************************************************************
 * Copyright (c) 2005-2009, G. Weirich and Elexis
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * Eine Baumförmige rekursive Datenstruktur. Ein Tree ist gleicheitig ein node. Ein Tree hat
 * children (die allerdings auch null sein können) und Geschwister, (die ebenfalls null sein
 * können), sowie ein Parent, welches ebenfalls null sein kann (dann ist dieses Tree-Objekt die
 * Wurzel des Baums) Jeder Tree trägt ein beliebiges Datenobjekt (contents).
 */
public class Tree<T> {
	public IFilter filter;
	protected Tree<T> parent;
	protected Tree<T> first;
	protected Tree<T> next;
	// protected Tree<T> last;
	public T contents;
	
	/**
	 * Eine neues Tree-Objekt erstellen
	 * 
	 * @param p
	 *            der Parent, oder null, wenn dies die Wurzel werden soll.
	 * @param elem
	 *            das zugeordnete Datenobjekt
	 */
	public Tree(Tree<T> p, T elem){
//System.out.println("js: Tree: Tree(p, elem) begin: Ein neues Tree-Objekt erstellen.");
		contents = elem;
		parent = p;
		first = null;
		// last=null;
		filter = null;
		if (parent != null) {
			next = parent.first;
			parent.first = this;
		}
//System.out.println("js: Tree: Tree(p, elem) end");
	}
	
	/**
	 * Ein neues Tree-Objekt innerhalb der Geschwisterliste sortiert einfügen
	 * 
	 * @param parent
	 *            Parent
	 * @param elem
	 *            Datenobjekt
	 * @param comp
	 *            Ein Comparator für das Fatenobjekt
	 */
	public Tree(Tree<T> parent, T elem, Comparator<T> comp){
//System.out.println("js: Tree: Tree(parent, elem, comp) begin: Ein neues Tree-Objekt innerhalb der Geschwsterliste sortiert einfügen...");
		this.parent = parent;
		contents = elem;
		if (parent != null) {
			next = parent.first;
			Tree<T> prev = null;
			while ((next != null) && (comp.compare(next.contents, elem) < 0)) {
				prev = next;
				next = next.next;
			}
			if (prev == null) {
				parent.first = this;
			} else {
				prev.next = this;
			}
		}
//System.out.println("js: Tree: Tree(parent, elem, comp) end");
	}
	
	/**
	 * Ein neues Tree-Objekt mit einem Filter erstellen. Wenn ein Filter gesetzt wird, dann werden
	 * von getChildren() nur die geliefert, die dem Filter entsprechen
	 * 
	 * @param p
	 *            Parent-Element
	 * @param elem
	 *            Datenobjekt
	 * @param f
	 *            Filter
	 */
	public Tree(Tree<T> p, T elem, IFilter f){
		this(p, elem);
//System.out.println("js: Tree: Tree(p, elem, f) Just returned from constructor: Ein neues Tree-Objekt mit einem Filter erstellen. Wenn ein Filter gesetzt wird, dann werden von getChildren() nur die geliefert, die dem Filter entsprechen.");
		filter = f;
//System.out.println("js: Tree: Tree(p, elem, f) Ein neues Tree-Objekt mit einem Filter erstellen. Wenn ein Filter gesetzt wird, dann werden von getChildren() nur die geliefert, die dem Filter entsprechen.");
	}
	
	/**
	 * Filter nachträglich setzen. Der Filter wird für dieses und alle Children gesetzt.
	 * 
	 * @param f
	 *            der Filter
	 */
	public void setFilter(IFilter f){
//System.out.println("js: Tree: setFilter(f) Filter nachträglich setzen. Der Filter wird für dieses und alle Children gesetzt.");
		filter = f;
		Tree<T> cursor = first;
		while (cursor != null) {
			cursor.setFilter(f);
			cursor = cursor.next;
		}
//System.out.println("js: Tree: setFilter(f) end");
	}
	
	/**
	 * Ein Datenobjekt als Kind-element zufügen. Dies (Das Datenobjekt wird implizit in ein
	 * Tree-Objekt gepackt. obj.add(t) ist dasselbe wie new Tree(obj,t))
	 * 
	 * @param elem
	 *            Das Datenobjekt
	 * @return das erzeugte Tree-Objekt
	 */
	public Tree<T> add(T elem){
//System.out.println("js: Tree: add(elem) begin: Ein Datenobjekt als Kind-element zufügen. Dies (Das Datenobjekt wird implizit in ein Tree-Objekt gepackt. obj.add(t) ist dasselbe wie new Tree(obj,t)");
		Tree<T> ret = new Tree<T>(this, elem, filter);
//System.out.println("js: Tree: add(elem) end - about to return Tree<T> ret");
		return ret;
	}
	
	/**
	 * Ein Kind-Element samt dessen Unterelementen entfernen
	 * 
	 * @param subtree
	 *            das Kindelement
	 */
	public void remove(Tree<T> subtree){
//System.out.println("js: Tree: remove(subtree) begin: Ein Kind-Element samt dessen Unterelementen entfernen.");
		if (first == null) {
//System.out.println("js: Tree: end - first == null - about to return early 1");
			return;
		}
		if (first.equals(subtree)) {
			first = subtree.next;
//System.out.println("js: Tree: end - first.equals(subtree);first = subtree.next - about to return early 2");
			return;
		}
		
		Tree<T> runner = first;
		
		while (!runner.next.equals(subtree)) {
			runner = runner.next;
			if (runner == null) {
//System.out.println("js: Tree: end - runner == null - about to return early 3");
				return;
			}
		}
		runner.next = subtree.next;
//System.out.println("js: Tree: remove(subtree) end  - runner.next = subtree next");
	}
	
	/**
	 * An einen anderen Parenet-Node oder Tree zügeln (Mitsamt allen Kindern)
	 * 
	 * @param newParent
	 *            der neue Elter
	 */
	public synchronized Tree<T> move(Tree<T> newParent){
//System.out.println("js: Tree: move(newParent) begin - An einen anderen Parenet-Node oder Tree zügeln (Mitsamt allen Kindern)");
		Tree<T> oldParent = parent;
		if (oldParent != null) {
			oldParent.remove(this);
		}
		parent = newParent;
		next = newParent.first;
		newParent.first = this;
//System.out.println("js: Tree: move(newParent) end - about to return this");
		return this;
	}
	
	/**
	 * Ähnlich wie add, aber wenn das übergebene Child schon existiert, werden nur dessen Kinder mit
	 * den Kindern des existenten childs ge'merged' (Also im Prinzip ein add mit Vermeidung von
	 * Dubletten
	 */
	public synchronized void merge(Tree<T> newChild){
//System.out.println("js: Tree: merge(newChild) begin - Ähnlich wie add, aber wenn das übergebene Child schon existiert, werden nur dessen Kinder mit den Kindern des existenten childs ge'merged' (Also im Prinzip ein add mit Vermeidung von Dubletten");
		Tree<T> tExist = find(newChild.contents, false);
		if (tExist != null) {
			for (Tree<T> ts = newChild.first; ts != null; ts = ts.next) {
				tExist.merge(ts);
			}
			if (newChild.first == null) {
				newChild.getParent().remove(newChild);
			}
		} else {
			newChild.move(this);
		}
//System.out.println("js: Tree: merge(newChild) end");		
	}
	
	/**
	 * Alle Kind-Elemente entfernen
	 * 
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	public synchronized void clear(){
//System.out.println("js: Tree: clear() begin - Alle Kind-Elemente entfernen");
		for (Tree t : getChildren()) {
			remove(t);
		}
//System.out.println("js: Tree: clear() end");
	}
	
	/**
	 * Alle Kind-Elemente liefern
	 * 
	 * 20130530js: WARNING: Elemente werden zurückgeliefert,
	 * wenn filter true liefert, ODER wenn cursor.hasChildren()!!!
	 * 
	 * @return eine Collection mit den Kind-Trees
	 */
	public Collection<Tree<T>> getChildren(){
//System.out.println("js: Tree: getChildren() begin - Alle Kind-Elemente liefern");

		ArrayList<Tree<T>> al = new ArrayList<Tree<T>>();
		Tree<T> cursor = first;

//if (cursor != null)	{ System.out.println("js: Tree: getChildren() - first element found..."); }
//else				{ System.out.println("js: Tree: getChildren() - WARNING: No element found at all."); }
		
		while (cursor != null) {
//System.out.println("js: Tree: getChildren() - cursor: "+cursor.toString());

			if (filter == null) {
//System.out.println("js: Tree: getChildren() - INFO: filter == null");
				al.add(cursor);
			} else {
				
//System.out.println("js: Tree: getChildren() - INFO: filter != null");
//System.out.println("js: Tree: WARNING: getChildren() will return children if filter returns true OR cursor.hasChildren!!!");

				if (filter.select(cursor.contents) || cursor.hasChildren()) {
//System.out.println("js: Tree: getChildren() - INFO: filter returned true OR cursor.hasChildren(), so about to al.add(cursor)...");
					al.add(cursor);
				} else {
//System.out.println("js: Tree: getChildren() - INFO: filter returned false");
				}
			}
			cursor = cursor.next;
		}
//System.out.println("js: Tree: getChildren() end - about to return al");
		return al;
	}
	
	/**
	 * Das Elternobjekt liefern
	 * 
	 * @return das parent
	 */
	public Tree<T> getParent(){
//System.out.println("js: Tree: getParent() begin, end - about to return parent");
		return parent;
	}
	
	/**
	 * Erstes Kind-element liefern. Null, wenn keine Kinder. Dies macht im Gegensatz zu
	 * hasChildren() keine synchronisation!
	 * 
	 * @return
	 */
	public Tree<T> getFirstChild(){
//System.out.println("js: Tree: getFirstChild() begin, end - about to return first");
		return first;
	}
	
	/**
	 * Nächstes Geschwister liefern oder null wenn keine mehr da sind. getParent().getFirstChild()
	 * liefert den Start der Geschwisterliste.
	 * 
	 * @return
	 */
	public Tree<T> getNextSibling(){
//System.out.println("js: Tree: getNextSibling() begin, end - about to return next");
		return next;
	}
	
	/**
	 * Fragen, ob Kinder vorhanden sind
	 * 
	 * @return true wenn dieses Objekt Children hat.
	 */
	public boolean hasChildren(){
//System.out.println("js: Tree: hasChildren() begin - Fragen, ob Kinder vorhanden sind");
		if (filter == null) {
//System.out.println("js: Tree: hasChildren() end - if filter == null - about to return first != null");
			return (first != null);
		}
		Tree<T> cursor = first;
		while (cursor != null) {
			if (filter.select(cursor.contents) || cursor.hasChildren()) {
//System.out.println("js: Tree: hasChildren() end - filter != null ... - about to return true");
				return true;
			}
			cursor = cursor.next;
		}
//System.out.println("js: Tree: hasChildren() end - filter != null ... - about to return false");
		return false;
	}
	
	/**
	 * Ein Array mit allen Elementen des Baums liefern
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	public Tree<T>[] toArray(){
//System.out.println("js: Tree: toArray() begin - Ein Array mit allen Elementen des Baums liefern");
//System.out.println("js: Tree: toArray() end - about to return Tree<T>[] getAll().toArray()");
		return (Tree<T>[]) getAll().toArray();
	}
	
	/**
	 * Eine Liste mit allen Elementen des Baums liefern
	 * 
	 * @return
	 */
	public Collection<Tree<T>> getAll(){
//System.out.println("js: Tree: getAll() begin - Eine Liste Array mit allen Elementen des Baums liefern");
		ArrayList<Tree<T>> al = new ArrayList<Tree<T>>();
		Tree<T> child = first;
		while (child != null) {
			al.add(child);
			al.addAll(child.getAll());
			child = child.next;
		}
//System.out.println("js: Tree: getAll() end - about to return al");
		return al;
	}
	
	public Tree<T> find(Object o, boolean deep){
//System.out.println("js: Tree: findObject(o, deep) begin");
		for (Tree<T> t : getChildren()) {
			if (t.contents.equals(o)) {
//System.out.println("js: Tree: findObject(o, deep) end - return t");
				return t;
			}
			if (deep) {
				Tree<T> ct = t.find(o, true);
				if (ct != null) {
//System.out.println("js: Tree: findObject(o, deep) end - return ct");
					return ct;
				}
			}
		}
//System.out.println("js: Tree: findObject(o, deep) end - return null");
		return null;
	}
	
}
