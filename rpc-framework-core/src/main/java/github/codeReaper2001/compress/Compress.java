package github.codeReaper2001.compress;

import github.codeReaper2001.extension.SPI;

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
