package ch.elexis.trustx;

import com4j.*;

@IID("{C941AF2D-3B68-3188-9432-861D91E393DB}")
public interface _ITrustxEvents extends Com4jObject {
	@VTID(3)
	void trustxEvent(ch.elexis.trustx.tagMsgLevel level, ch.elexis.trustx.tagMsgClass mclass,
		ch.elexis.trustx.tagMsgOrigin origin, int code, java.lang.String fileName,
		java.lang.String msg);
	
}
