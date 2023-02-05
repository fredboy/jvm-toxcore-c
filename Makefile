heroku:
	$(MAKE) -f scripts/build-host

publishM2:
	./scripts/build-host -j$(nproc)
	./scripts/build-aarch64-linux-android -j$(nproc) release
	./scripts/build-arm-linux-androideabi -j$(nproc) release
	./scripts/build-i686-linux-android -j$(nproc) release
	./scripts/build-x86_64-linux-android -j$(nproc) release
