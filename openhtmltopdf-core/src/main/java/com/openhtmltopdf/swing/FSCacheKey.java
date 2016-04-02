package com.openhtmltopdf.swing;

public class FSCacheKey {
	private final String uri;
	private final Class<?> clazz;
	
	public FSCacheKey(String uri, Class<?> clazz) {
		this.uri = uri;
		this.clazz = clazz;
	}
	
	public String getUri() {
		return this.uri;
	}
	
	public Class<?> getClazz() {
		return this.clazz;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FSCacheKey other = (FSCacheKey) obj;
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return this.clazz.getName() + ":" + this.uri;
	}
}
