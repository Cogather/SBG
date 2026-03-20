let recorder;
let mediaStream;
let client;
let state = ''

async function START_RECORDING(data) {
    if (state === 'recording') {
        throw new Error('Called startRecording while recording is in progress.');
    }
    state = 'recording'
    const codecMode = data.codecMode
    if (codecMode === 'webcodecs') {
        await startWCRecording(data)
    } else {
        await startFFRecording(data)
    }
}


function checkConnectionStatus() {
    return client && client.readyState === WebSocket.OPEN;
}


async function startWebSocket(data) {
    const {
        dataDealAddr,
        imeiAndImsi,
        appType,
        bitRate,
        sampleRate,
        channelCount,
        codecMode,
        frameRate
    } = data;

    // 创建WebSocket连接
    client = new WebSocket(
        `ws://${dataDealAddr}/browser/websocket/${imeiAndImsi}?appType=${appType}&bitRate=${bitRate}&sampleRate=${sampleRate}&channels=${channelCount}&codecMode=${codecMode}&frameRate=${frameRate}`,
        []
    );

    client.onclose = function (event) {
        console.log("websocket close ", event)
    };

    client.onerror = function (error) {
        console.log("websocket error ", error)
    };

    await new Promise((resolve) => {
        checkConnectionStatus() && resolve();
        client.addEventListener('open', () => {
            resolve();
        });
    });
}

async function startWCRecording(data) {
    try {
        await startWebSocket(data)
        const {
            // 音频相关
            sampleRate,
            channelCount,
            echoCancellation,
            noisesuppression,

            frameRate,
            width,
            height,
        } = data;

        const constraints = {
            video: true,
            audio: true,
            videoConstraints: {
                mandatory: {
                    minWidth: width,
                    maxWidth: width,
                    minHeight: height,
                    maxHeight: height,
                    minFrameRate: frameRate,
                    maxFrameRate: frameRate,
                }

            },
            audioConstraints: {
                mandatory: {
                    channelCount: channelCount,
                    sampleRate: sampleRate,
                    echoCancellation: echoCancellation,
                    noisesuppression: noisesuppression,
                }
            }
        }

        mediaStream = await new Promise((resolve, reject) => {
            chrome.tabCapture.capture(constraints, (stream) => {
                stream.getTracks().forEach(function (track) { });
                stream ? resolve(stream) : reject();
            })
        })

        await startAudioProcessing(data, mediaStream.getAudioTracks()[0]);
        await startVideoProcessing(data, mediaStream.getVideoTracks()[0]);
        console.log('Recording started');
    } catch (error) {
        console.error('Error starting recording:', error);
    }
}


async function startVideoProcessing(data, videoTrack) {
    console.log('[startVideoProcessing]:', JSON.stringify(data));

    // 获取视频约束参数
    const {
        // 音频相关
        frameRate,
        width,
        height,
        bitRate,
    } = data;

    // 视频编码器配置
    const encoderConfig = {
        codec: 'avc1.42E01F',
        width: width,
        height: height,
        bitrate: (width * height * bitRate) / 100,
        framerate: frameRate,
        latencyMode: 'realtime',
        avc: { format: 'annexb' },
    };

    // 检查编码器配置是否支持
    if (!(await VideoEncoder.isConfigSupported(encoderConfig)).supported) {
        throw new Error('当前浏览器不支持所需编解码器配置');
    }

    // 创建视频编码器
    const videoEncoder = new VideoEncoder({
        output: handleVideoChunk,
        error: (error) => console.error('视频编码错误', error),
    });

    videoEncoder.configure(encoderConfig);

    // 创建视频轨道处理器
    const videoTrackReader = new MediaStreamTrackProcessor({ track: videoTrack }).readable.getReader();
    let frameCounter = 0;

    // 处理视频帧
    async function processVideoFrames() {
        while (true) {
            const { value: videoFrame, done: isDone } = await videoTrackReader.read();

            if (isDone) {
                break;
            }

            // 每10帧生成一个关键帧
            videoEncoder.encode(videoFrame, { keyFrame: frameCounter % 10 === 0 });
            frameCounter++;
            videoFrame.close();
        }
    }

    processVideoFrames();
}

function handleVideoChunk(videoChunk) {
    // 创建视频帧类型标记
    const frameTypeMarker = new Uint8Array(2);
    frameTypeMarker[0] = 1;
    frameTypeMarker[1] = videoChunk.type === 'key' ? 1 : 0;

    // 复制视频数据
    const videoData = new Uint8Array(videoChunk.byteLength);
    videoChunk.copyTo(videoData);

    // 合并并发送数据
    combineAndSendBuffer(frameTypeMarker, videoData);
}

function combineAndSendBuffer(header, payload) {
    const combinedBuffer = new Uint8Array(header.byteLength + payload.byteLength);
    combinedBuffer.set(header, 0);
    combinedBuffer.set(payload, header.length);

    // 如果连接正常则发送数据
    checkConnectionStatus() && client.send(combinedBuffer.buffer);
}

async function startAudioProcessing(data, audioTrack) {
    const {
        // 音频相关
        sampleRate,
        channelCount,
    } = data;
    // 创建音频上下文
    const audioContext = new AudioContext({
        sampleRate: sampleRate,
        latencyHint: 'interactive',
        renderSize: 128,
        numberOfChannels: channelCount,
    });

    // 添加音频处理器
    await audioContext.audioWorklet.addModule('audio-processor.js');

    // 创建音频处理节点
    const audioProcessorNode = new AudioWorkletNode(audioContext, 'mp3-encoder', {
        processorOptions: {
            channel: channelCount,
            sampleRate: sampleRate,
            bitrate: 80,
        },
    });

    // 连接音频源到处理器
    audioContext.createMediaStreamSource(new MediaStream([audioTrack])).connect(audioProcessorNode);

    console.log('track.getSettings().sampleRate:', audioTrack.getSettings().sampleRate);
    console.log('audioCtx.sampleRate:', audioContext.sampleRate);

    // 处理编码后的音频数据
    audioProcessorNode.port.onmessage = (event) => {
        const { mp3buffer: mp3Buffer, timestamp: timeStamp } = event.data;

        if (!mp3Buffer.byteLength) {
            return;
        }

        // 创建音频标记
        const audioMarker = new Uint8Array(1);
        audioMarker[0] = 2;

        // 合并并发送音频数据
        combineAndSendBuffer(audioMarker, mp3Buffer);
    };
}


async function startFFRecording(data) {
    try {


        await startWebSocket(data)
        const {
            // 音频相关
            sampleRate,
            channelCount,
            echoCancellation,
            noisesuppression,
            bitRate,
            frameRate,
            width,
            height,
        } = data;

        const constraints = {
            video: true,
            audio: true,
            videoConstraints: {
                mandatory: {
                    minWidth: width,
                    maxWidth: width,
                    minHeight: height,
                    maxHeight: height,
                    minFrameRate: frameRate,
                    maxFrameRate: frameRate,
                }

            },
            audioConstraints: {
                mandatory: {
                    channelCount: channelCount,
                    sampleRate: sampleRate,
                    echoCancellation: echoCancellation,
                    noisesuppression: noisesuppression,
                }
            }
        }

        mediaStream = await new Promise((resolve, reject) => {
            chrome.tabCapture.capture(constraints, (stream) => {
                stream.getTracks().forEach(function (track) { });
                stream ? resolve(stream) : reject();
            })
        })

        // 配置MediaRecorder
        const recorderOptions = {
            ignoreMutedMedia: true,
            audioBitsPerSecond: 64000, // 固定音频码率
            videoBitsPerSecond: (width * height * bitRate) / 100,
            // 自动降级不支持的媒体类型
            mimeType: 'video/webm;codecs="vp8,opus"'
        };

        recorder = new MediaRecorder(mediaStream, recorderOptions);
        setupRecorderDataHandling(recorder)
        recorder.start(50);
    } catch (error) {
        console.error('Error starting recording:', error);
    }
}

function setupRecorderDataHandling(recorder) {
    let bufferList = [];
    let isFirstChunk = true;
    const chunkThreshold = 1; // 数据块合并阈值

    // 处理可用数据
    recorder.ondataavailable = async (event) => {
        if (!event.data.size) {
            return; // 忽略空数据
        }

        const dataBuffer = await event.data.arrayBuffer();
        const totalBufferSize = bufferList.reduce((sum, buf) => sum + buf.byteLength, 0);

        // 处理数据块合并逻辑
        if (isFirstChunk && totalBufferSize < chunkThreshold) {
            bufferList.push(dataBuffer);
        } else {
            if (checkConnectionStatus()) {
                // 发送合并的初始数据块
                if (isFirstChunk) {
                    const mergedBuffer = mergeArrayBuffers(...bufferList);
                    client.send(mergedBuffer);
                    isFirstChunk = false;
                    console.log('发送合并数据，大小:', mergedBuffer.byteLength);
                }
                // 发送当前数据块
                client.send(dataBuffer);
            }
        }
    };

    // 错误处理
    recorder.onerror = (error) => {
        console.error('录制器错误:', error);
        recorder.stop();
    };

    // 停止事件
    recorder.onstop = () => {
        console.log('录制已停止');
    };

    // 流失效处理
    mediaStream.oninactive = () => {
        try {
            if (recorder.state !== 'inactive') {
                recorder.stop();
            }
        } catch (error) {
            console.error('流失效时停止录制出错:', error);
        }
    };
}

function mergeArrayBuffers(...e) {
    const t = e.reduce((e, t) => e + t.byteLength, 0),
      r = new ArrayBuffer(t),
      n = new Uint8Array(r);
    let o = 0;
    return (
      e.forEach((e) => {
        n.set(new Uint8Array(e), o), (o += e.byteLength);
      }),
      r
    );
}

async function STOP_RECORDING() {
    if (mediaStream) {
        mediaStream.getTracks().forEach((t) => t.stop());
        mediaStream = null;
    }
    if (client) {
        client.close()
        client = null;
    }
    if (recorder) {
        recorder.stop();
        recorder = null;
    }
    console.log('Recording stopped');
} 