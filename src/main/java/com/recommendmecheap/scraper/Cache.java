package com.recommendmecheap.scraper;

public interface Cache<T> {
	public T Load();	
	public boolean Invalidate();
	public boolean IsExists();
	public void Write(T t);
}