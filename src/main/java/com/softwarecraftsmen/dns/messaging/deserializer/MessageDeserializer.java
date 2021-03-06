/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging.deserializer;

import com.softwarecraftsmen.dns.messaging.Question;
import com.softwarecraftsmen.dns.messaging.Message;
import com.softwarecraftsmen.dns.messaging.MessageHeader;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import static com.softwarecraftsmen.dns.messaging.MessageHeader.SizeOfDnsMessageHeader;
import com.softwarecraftsmen.dns.resourceRecords.ResourceRecord;
import com.softwarecraftsmen.dns.names.Name;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import java.util.ArrayList;
import java.util.List;
import static java.util.Locale.UK;

public class MessageDeserializer
{
	private final StructureReader reader;

	public MessageDeserializer(final @NotNull byte[] message) throws BadlyFormedDnsMessageException
	{
		if (message.length < SizeOfDnsMessageHeader)
		{
			throw new BadlyFormedDnsMessageException("The message is less than the length of the fixed size DNS MessageHeader");
		}
		this.reader = new StructureReader(new AtomicReader(new ByteArrayReader(message)));
	}

	@NotNull
	public Message readMessage() throws BadlyFormedDnsMessageException, TruncatedDnsMessageException
	{
		final MessageHeader messageHeader = reader.readMessageHeader();
		final List<Question> questionList = readQuestions(messageHeader.getNumberOfEntriesInQuestionSection());
		final List<ResourceRecord<? extends Name, ? extends Serializable>> answers = readResourceRecords(messageHeader.getNumberOfResourceRecordsInAnswerSection());
		final List<ResourceRecord<? extends Name, ? extends Serializable>> nameServerAuthorities = readResourceRecords(messageHeader.getNumberOfNameServerRecordsInAuthoritySection());
		final List<ResourceRecord<? extends Name, ? extends Serializable>> additionalRecords = readResourceRecords(messageHeader.getNumberOfResourceRecordsInAdditionalRecordsAnswerSection());
		return new Message
		(
			messageHeader,
			questionList,
			answers,
			nameServerAuthorities,
			additionalRecords
		);
	}

	@NotNull
	private List<Question> readQuestions(final @NotNull Unsigned16BitInteger numberOfQuestions) throws BadlyFormedDnsMessageException
	{
		final List<Question> questions = new ArrayList<Question>();
		final long longNumberOfQuestions = numberOfQuestions.toLong();
		long index = 0;
		while(index++ < longNumberOfQuestions)
		{
			questions.add(reader.readQuestion());
		}
		return questions;
	}

	@NotNull
	private List<ResourceRecord<? extends Name, ? extends Serializable>> readResourceRecords(final @NotNull Unsigned16BitInteger numberOfResourceRecords) throws BadlyFormedDnsMessageException
	{
		final List<ResourceRecord<? extends Name, ? extends Serializable>> resourceRecords = new ArrayList<ResourceRecord<? extends Name, ? extends Serializable>>();
		final long longNumberOfResourceRecords = numberOfResourceRecords.toLong();
		long index = 0;
		while(index++ < longNumberOfResourceRecords)
		{
			try
			{
				resourceRecords.add(reader.readResourceRecord());
			}
			catch(BadlyFormedDnsMessageException cause)
			{
				throw new BadlyFormedDnsMessageException(format(UK, "Failed to read resource record number %1$s of %2$s", index, numberOfResourceRecords), cause);
			}
		}
		return resourceRecords;
	}
}
