package net.tomp2p.rpc;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.origin.Sctp;
import net.tomp2p.futures.FutureDone;
import net.tomp2p.message.Message;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.builder.SendDirectBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerSocketAddress;
import net.tomp2p.rpc.RelayRPC;
import net.tomp2p.utils.Pair;
import net.tomp2p.utils.Triple;

public class TestRelayRPC {
	
	@Rule
    public TestRule watcher = new TestWatcher() {
	   protected void starting(Description description) {
          System.out.println("Starting test: " + description.getMethodName());
       }
    };
	
	@Test
	public void testSetupRelay() throws InterruptedException {
		Sctp.getInstance().init();
		Peer behindNAT1Peer = null;
		Peer behindNAT2Peer = null;
		Peer relayPeer = null;
		try {
			behindNAT1Peer = new PeerBuilder(new Number160("0x1234")).p2pId(55).enableMaintenance(false).port(1234).start();
			RelayRPC behindNAT1Relay = new RelayRPC(behindNAT1Peer);
			DirectDataRPC d1 = new DirectDataRPC(behindNAT1Peer.peerBean(), behindNAT1Peer.connectionBean());
			
			behindNAT2Peer = new PeerBuilder(new Number160("0x9876")).p2pId(55).enableMaintenance(false).port(9876).start();
			RelayRPC behindNAT2Relay = new RelayRPC(behindNAT2Peer);
			DirectDataRPC d2 = new DirectDataRPC(behindNAT2Peer.peerBean(), behindNAT2Peer.connectionBean());
			
			relayPeer = new PeerBuilder(new Number160("0x5665")).p2pId(55).enableMaintenance(false).port(5665).start();
			RelayRPC relayRelay = new RelayRPC(relayPeer);
			
			FutureDone<Message> future = behindNAT1Relay.sendSetupMessage(relayPeer.peerAddress()).element0();
			future.awaitUninterruptibly();
			Assert.assertTrue(future.isSuccess());
			Collection<PeerSocketAddress> pa = new ArrayList<>();
			pa.add(relayPeer.peerAddress().ipv4Socket());
			behindNAT1Peer.peerBean().serverPeerAddress(behindNAT1Peer.peerAddress().withRelays(pa).withReachable4UDP(false).withHolePunching(true));
			
			behindNAT2Peer.peerBean().serverPeerAddress(behindNAT2Peer.peerAddress().withReachable4UDP(false).withHolePunching(true));
			SendDirectBuilder s = new SendDirectBuilder(behindNAT2Peer, behindNAT1Peer.peerAddress());
			Pair<FutureDone<Message>, FutureDone<SctpChannelFacade>> fr = d2.send(behindNAT1Peer.peerAddress(), s);
			fr.element0().awaitUninterruptibly();
			Assert.assertTrue(fr.element0().isSuccess());
			
			System.err.println(fr.element0().failedReason());
		
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
            if (behindNAT1Peer != null) {
            	behindNAT1Peer.shutdown().await();
            }
            if (behindNAT2Peer != null) {
            	behindNAT2Peer.shutdown().await();
            }
            if (relayPeer != null) {
            	relayPeer.shutdown().await();
            }
        }
	}
}
