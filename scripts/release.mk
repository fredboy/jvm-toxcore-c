_build/$(TARGET)/tox4j/libtox4j-c$(DLLEXT): $(PREFIX)/tox4j.stamp
	ls -l $@
	touch $@

release: _build/$(TARGET)/tox4j/libtox4j-c$(DLLEXT)
	rm -rf $(wildcard cpp/src/main/resources/im/tox/tox4j/impl/jni/*/)
	mkdir -p cpp/src/main/resources/im/tox/tox4j/impl/jni/$(TOX4J_PLATFORM)/
	cp $< cpp/src/main/resources/im/tox/tox4j/impl/jni/$(TOX4J_PLATFORM)/
	cd cpp && sbt publishM2